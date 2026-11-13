package neatlogic.module.deploy.service;

import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.*;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import javax.annotation.Resource;
import java.util.Objects;

/** 持续集成共用原有校验，事务只覆盖正式保存及独立的 GitLab 同步。 */
@Service
public class DeployCiWriter {
    private static final Logger logger = LoggerFactory.getLogger(DeployCiWriter.class);
    @Resource private DeployCiMapper deployCiMapper;
    @Resource private DeployCiService deployCiService;
    @Resource private DeployAppAuthorityService deployAppAuthorityService;

    /** 管理页面同步保存；接入申请可以将 Webhook 延迟到本地提交之后。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(DeployCiVo deployCiVo, boolean deferWebhook) throws Exception {
        deployAppAuthorityService.checkOperationAuth(deployCiVo.getAppSystemId(), DeployAppConfigAction.EDIT);
        validateDefinition(deployCiVo);
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo system = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployCiVo.getAppSystemId());
        if (system == null) {
            throw new CiEntityNotFoundException(deployCiVo.getAppSystemId());
        }
        CiEntityVo module = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployCiVo.getAppModuleId());
        if (module == null) {
            throw new CiEntityNotFoundException(deployCiVo.getAppModuleId());
        }
        DeployCiVo ci = deployCiMapper.getDeployCiById(deployCiVo.getId());
        if (DeployCiRepoType.GITLAB.getValue().equals(deployCiVo.getRepoType())) {
            String gitlabPassword = deployCiVo.getConfig().getString("gitlabPassword");
            gitlabPassword = RC4Util.encrypt(gitlabPassword);
            deployCiVo.getConfig().put("gitlabPassword", gitlabPassword);
            if (!deferWebhook) {
                syncWebhook(deployCiVo, ci, system, module, false);
            }
        }
        deployCiMapper.insertDeployCi(deployCiVo);
    }

    /** CI 只读校验不声明事务，预检可返回字段错误，保存失败仍由写入事务回滚。 */
    public void validateDefinition(DeployCiVo value) {
        if (deployCiMapper.checkDeployCiIsRepeat(value) > 0) {
            throw new DeployCiIsRepeatException(value.getName());
        }
        if (DeployCiRepoType.GITLAB.getValue().equals(value.getRepoType())
                && (value.getConfig() == null || StringUtils.isBlank(value.getConfig().getString("gitlabUsername"))
                || StringUtils.isBlank(value.getConfig().getString("gitlabPassword")))) {
            throw new DeployCiGitlabAccountLostException(value.getRepoServerAddress(), value.getRepoName());
        }
        if (DeployCiRepoType.GITLAB.getValue().equals(value.getRepoType())) webhookCallbackHost();
    }

    /** 接入预检与正式同步共用回调地址检查，不发起外部请求。 */
    private String webhookCallbackHost() {
        String gitlabWebHookCallbackHostValue = ConfigManager.getConfig(DeployTenantConfig.GITLAB_WEB_HOOK_CALLBACK_HOST);
        if (StringUtils.isBlank(gitlabWebHookCallbackHostValue)) {
            throw new DeployGitlabWebHookCallbackHostLostException();
        }
        // gitlabWebHookCallbackHost格式形如：http://192.168.0.25:8080/neatlogic
        if (!RegexUtils.isMatch(gitlabWebHookCallbackHostValue, RegexUtils.CONNECT_URL)) {
            throw new DeployGitlabWebHookCallbackHostIlegalException();
        }
        return gitlabWebHookCallbackHostValue;
    }

    /** 根据已入库的 CI 配置同步，成功后仅写回 Webhook ID。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronize(Long id) throws Exception {
        DeployCiVo ci = deployCiMapper.getDeployCiById(id);
        if (ci == null) {
            throw new DeployCiNotFoundException(id);
        }
        deployAppAuthorityService.checkOperationAuth(ci.getAppSystemId(), DeployAppConfigAction.EDIT);
        ICiEntityCrossoverMapper entities = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        syncWebhook(ci, ci, entities.getCiEntityBaseInfoById(ci.getAppSystemId()), entities.getCiEntityBaseInfoById(ci.getAppModuleId()), true);
        deployCiMapper.updateHook(id, ci.getHookId());
    }

    /** 复用 Runner 连接、回调地址及变更仓库时的旧钩子处理。 */
    private void syncWebhook(DeployCiVo deployCiVo, DeployCiVo ci, CiEntityVo system, CiEntityVo module, boolean reconcile) throws Exception {
        RunnerVo runnerVo = deployCiService.getRandomRunnerBySystemIdAndModuleId(system, module);
        JSONObject param = new JSONObject();
        param.put("ciId", deployCiVo.getId());
        if (reconcile) param.put("reconcile", true);
        if (ci != null) {
            param.put("hookId", ci.getHookId());
            // 检查服务器与仓库是否有变化，有则删除原有的hook，再生成新的hook
            if (!Objects.equals(deployCiVo.getRepoServerAddress(), ci.getRepoServerAddress()) || !Objects.equals(deployCiVo.getRepoName(), ci.getRepoName())) {
                deployCiService.deleteGitlabWebHook(ci, runnerVo.getUrl());
            }
        }
        param.put("callbackHost", webhookCallbackHost());
        param.put("repoServerAddress", deployCiVo.getRepoServerAddress());
        param.put("repoName", deployCiVo.getRepoName());
        param.put("branchFilter", deployCiVo.getBranchFilter());
        param.put("event", deployCiVo.getEvent());
        param.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
        param.put("username", deployCiVo.getConfig().getString("gitlabUsername"));
        param.put("password", deployCiVo.getConfig().getString("gitlabPassword"));
        String url = runnerVo.getUrl() + "/api/rest/deploy/ci/gitlabwebhook/save";
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        String errorMsg = request.getErrorMsg();
        JSONObject resultJson = request.getResultJson();
        if (StringUtils.isNotBlank(errorMsg)) {
            logger.error("GitLab Webhook 同步失败，CI ID={}", deployCiVo.getId());
            throw new DeployCiGitlabWebHookSaveFailedException(errorMsg);
        }
        deployCiVo.setHookId(resultJson.getString("Return"));
    }
}
