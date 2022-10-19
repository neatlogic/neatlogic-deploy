package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployCiGitlabAuthMode;
import codedriver.framework.deploy.constvalue.DeployCiRepoType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.exception.DeployCiGitlabAccountLostException;
import codedriver.framework.deploy.exception.DeployCiGitlabWebHookDeleteFailedException;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import codedriver.module.deploy.service.DeployCiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class DeleteDeployCiApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(DeleteDeployCiApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployCiService deployCiService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "删除持续集成配置";
    }

    @Override
    public String getToken() {
        return "deploy/ci/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true),
    })
    @Description(desc = "删除持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployCiVo ci = deployCiMapper.getDeployCiById(id);
        if (ci != null) {
            deployAppAuthorityService.checkOperationAuth(ci.getAppSystemId(), DeployAppConfigAction.EDIT);
            if (DeployCiRepoType.GITLAB.getValue().equals(ci.getRepoType())) {
                ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                CiEntityVo system = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(ci.getAppSystemId());
                if (system == null) {
                    throw new CiEntityNotFoundException(ci.getAppSystemId());
                }
                CiEntityVo module = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(ci.getAppModuleId());
                if (module == null) {
                    throw new CiEntityNotFoundException(ci.getAppModuleId());
                }
                RunnerVo runnerVo = deployCiService.getRandomRunnerBySystemIdAndModuleId(system, module);
                String gitlabUsername = ci.getConfig().getString("gitlabUsername");
                String gitlabPassword = ci.getConfig().getString("gitlabPassword");
                if (StringUtils.isBlank(gitlabUsername) || StringUtils.isBlank(gitlabPassword)) {
                    throw new DeployCiGitlabAccountLostException();
                }
                JSONObject param = new JSONObject();
                param.put("hookId", ci.getHookId());
                param.put("repoServerAddress", ci.getRepoServerAddress());
                param.put("repoName", ci.getRepoName());
                param.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
                param.put("username", gitlabUsername);
                param.put("password", gitlabPassword);
                String url = runnerVo.getUrl() + "/api/rest/deploy/ci/gitlabwebhook/delete";
                HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
                String errorMsg = request.getErrorMsg();
                if (StringUtils.isNotBlank(errorMsg)) {
                    logger.error("Gitlab webhook delete failed. Request url: {}; params: {}; errorMsg: {}", url, param.toJSONString(), errorMsg);
                    throw new DeployCiGitlabWebHookDeleteFailedException();
                }
            }
        }
        deployCiMapper.deleteDeployCiById(id);
        deployCiMapper.deleteDeployCiAuditByCiId(id);
        return null;
    }

}
