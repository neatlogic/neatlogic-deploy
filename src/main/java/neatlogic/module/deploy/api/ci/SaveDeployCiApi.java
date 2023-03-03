package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.ConfigMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.*;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.ConfigVo;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployCiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployCiApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(SaveDeployCiApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    ConfigMapper configMapper;

    @Resource
    DeployCiService deployCiService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "保存持续集成配置";
    }

    @Override
    public String getToken() {
        return "deploy/ci/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "名称", rule = RegexUtils.NAME, maxLength = 50, type = ApiParamType.REGEX, isRequired = true),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.ENUM, rule = "0,1", isRequired = true),
            @Param(name = "appSystemId", desc = "应用ID", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "appModuleId", desc = "模块ID", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "repoType", desc = "仓库类型", member = DeployCiRepoType.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "repoServerAddress", desc = "仓库服务器地址", rule = RegexUtils.CONNECT_URL, type = ApiParamType.REGEX, maxLength = 100, isRequired = true),
            @Param(name = "repoName", desc = "仓库名称", rule = RegexUtils.NAME_WITH_SLASH, maxLength = 50, type = ApiParamType.REGEX, isRequired = true),
            @Param(name = "branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "event", desc = "事件", member = DeployCiRepoEvent.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "action", desc = "动作类型", member = DeployCiActionType.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerType", member = DeployCiTriggerType.class, desc = "触发类型", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerTime", desc = "触发时间", type = ApiParamType.STRING),
            @Param(name = "delayTime", desc = "延迟时间", type = ApiParamType.INTEGER),
            @Param(name = "versionRule", desc = "版本号规则", type = ApiParamType.JSONOBJECT),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "保存持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiVo deployCiVo = paramObj.toJavaObject(DeployCiVo.class);
        deployAppAuthorityService.checkOperationAuth(deployCiVo.getAppSystemId(), DeployAppConfigAction.EDIT);
        if (deployCiMapper.checkDeployCiIsRepeat(deployCiVo) > 0) {
            throw new DeployCiIsRepeatException(deployCiVo.getName());
        }
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
            RunnerVo runnerVo = deployCiService.getRandomRunnerBySystemIdAndModuleId(system, module);
            String gitlabUsername = deployCiVo.getConfig().getString("gitlabUsername");
            String gitlabPassword = deployCiVo.getConfig().getString("gitlabPassword");
            if (StringUtils.isBlank(gitlabUsername) || StringUtils.isBlank(gitlabPassword)) {
                throw new DeployCiGitlabAccountLostException(deployCiVo.getRepoServerAddress(), deployCiVo.getRepoName());
            }
            gitlabPassword = RC4Util.encrypt(gitlabPassword);
            deployCiVo.getConfig().put("gitlabPassword", gitlabPassword);
            JSONObject param = new JSONObject();
            param.put("ciId", deployCiVo.getId());
            if (ci != null) {
                param.put("hookId", ci.getHookId());
                // 检查服务器与仓库是否有变化，有则删除原有的hook，再生成新的hook
                if (!Objects.equals(deployCiVo.getRepoServerAddress(), ci.getRepoServerAddress()) || !Objects.equals(deployCiVo.getRepoName(), ci.getRepoName())) {
                    deployCiService.deleteGitlabWebHook(ci, runnerVo.getUrl());
                }
            }
            ConfigVo gitlabWebHookCallbackHost = configMapper.getConfigByKey("gitlabWebHookCallbackHost");
            if (gitlabWebHookCallbackHost == null || StringUtils.isBlank(gitlabWebHookCallbackHost.getValue())) {
                throw new DeployGitlabWebHookCallbackHostLostException();
            }
            // gitlabWebHookCallbackHost格式形如：http://192.168.0.25:8080/neatlogic
            if (!RegexUtils.isMatch(gitlabWebHookCallbackHost.getValue(), RegexUtils.CONNECT_URL)) {
                throw new DeployGitlabWebHookCallbackHostIlegalException();
            }
            param.put("callbackHost", gitlabWebHookCallbackHost.getValue());
            param.put("repoServerAddress", deployCiVo.getRepoServerAddress());
            param.put("repoName", deployCiVo.getRepoName());
            param.put("branchFilter", deployCiVo.getBranchFilter());
            param.put("event", deployCiVo.getEvent());
            param.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
            param.put("username", gitlabUsername);
            param.put("password", gitlabPassword);
            String url = runnerVo.getUrl() + "/api/rest/deploy/ci/gitlabwebhook/save";
            HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            String errorMsg = request.getErrorMsg();
            JSONObject resultJson = request.getResultJson();
            if (StringUtils.isNotBlank(errorMsg)) {
                logger.error("Gitlab webhook save failed. Request url: {}; params: {}; errorMsg: {}", url, param.toJSONString(), errorMsg);
                throw new DeployCiGitlabWebHookSaveFailedException(errorMsg);
            }
            deployCiVo.setHookId(resultJson.getString("Return"));
        }
        deployCiMapper.insertDeployCi(deployCiVo);
        return null;
    }

    public IValid name() {
        return value -> {
            DeployCiVo deployCiVo = value.toJavaObject(DeployCiVo.class);
            if (deployCiMapper.checkDeployCiIsRepeat(deployCiVo) > 0) {
                return new FieldValidResultVo(new DeployCiIsRepeatException(deployCiVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
