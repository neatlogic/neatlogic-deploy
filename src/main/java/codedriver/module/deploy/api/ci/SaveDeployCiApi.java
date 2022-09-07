package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.*;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.deploy.exception.DeployCiGitlabAccountLostException;
import codedriver.framework.deploy.exception.DeployCiGitlabWebHookSaveFailedException;
import codedriver.framework.deploy.exception.DeployCiIsRepeatException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.RegexUtils;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployCiApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

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
            @Param(name = "appSystemId", desc = "应用ID", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "appModuleId", desc = "模块ID", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "repoType", member = DeployCiRepoType.class, desc = "仓库类型", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "repoServerAddress", maxLength = 50, desc = "仓库服务器地址", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoName", maxLength = 50, desc = "仓库名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "event", member = DeployCiRepoEvent.class, desc = "事件", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "action", member = DeployCiActionType.class, desc = "动作类型", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerType", member = DeployCiTriggerType.class, desc = "触发类型", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerTime", desc = "触发时间", type = ApiParamType.STRING),
            @Param(name = "versionRule", desc = "版本号规则", type = ApiParamType.JSONOBJECT),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "保存持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiVo deployCiVo = paramObj.toJavaObject(DeployCiVo.class);
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
            RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(deployCiVo.getAppSystemId(), deployCiVo.getAppModuleId());
            if (runnerGroupVo == null || CollectionUtils.isEmpty(runnerGroupVo.getRunnerList())) {
                throw new DeployAppConfigModuleRunnerGroupNotFoundException(system.getName() + "(" + deployCiVo.getAppSystemId() + ")", module.getName() + "(" + deployCiVo.getAppModuleId() + ")");
            }
            List<RunnerVo> runnerList = runnerGroupVo.getRunnerList();
            int runnerMapIndex = (int) (Math.random() * runnerList.size());
            RunnerVo runnerVo = runnerList.get(runnerMapIndex);
            String gitlabUsername = deployCiVo.getConfig().getString("gitlabUsername");
            String gitlabPassword = deployCiVo.getConfig().getString("gitlabPassword");
            if (StringUtils.isBlank(gitlabUsername) || StringUtils.isBlank(gitlabPassword)) {
                throw new DeployCiGitlabAccountLostException();
            }
            JSONObject param = new JSONObject();
            param.put("ciId", deployCiVo.getId());
            if (ci != null) {
                param.put("hookId", deployCiVo.getHookId());
            }
            param.put("repoServerAddress", deployCiVo.getRepoServerAddress());
            param.put("repoName", deployCiVo.getRepoName());
            param.put("branchFilter", deployCiVo.getBranchFilter());
            param.put("event", deployCiVo.getEvent());
            param.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
            param.put("username", gitlabUsername);
            param.put("password", RC4Util.encrypt(gitlabPassword));
            HttpRequestUtil request = HttpRequestUtil.post(runnerVo.getUrl() + "/api/rest/deploy/ci/gitlabwebhook/save").setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            String error = request.getError();
            JSONObject resultJson = request.getResultJson();
            if (StringUtils.isNotBlank(error)) {
                throw new DeployCiGitlabWebHookSaveFailedException();
            }
            if (ci == null) {
                deployCiVo.setHookId(resultJson.getString("Return"));
            }
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
