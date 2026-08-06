package neatlogic.module.deploy.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployCiGitlabAuthMode;
import neatlogic.framework.deploy.constvalue.DeployCiRepoEvent;
import neatlogic.framework.deploy.exception.DeployGitlabHookLessRepoNameException;
import neatlogic.framework.deploy.exception.DeployGitlabHookRepoNameLessParamException;
import neatlogic.framework.deploy.exception.DeployGitlabHookRepoNameUnknownActionException;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class BatchUpdateGitlabHookApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmdac.batchupdategitlabhookapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/ci/gitlabwebhook/batchupdate";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "runnerUrl", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.runnerurl", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoServerAddress", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.reposerveraddress", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "callbackUrl", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.callbackurl", type = ApiParamType.JSONARRAY),
            @Param(name = "branchFilter", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.branchfilter", type = ApiParamType.STRING),
            @Param(name = "username", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.username", type = ApiParamType.STRING),
            @Param(name = "password", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.password", type = ApiParamType.STRING),
            @Param(name = "action", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.action", type = ApiParamType.ENUM, rule = "insert,delete"),
            @Param(name = "configList", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.name", type = ApiParamType.JSONARRAY, isRequired = true),
            @Param(name = "configList.repoServerAddress", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.reposerveraddress"),
            @Param(name = "configList.repoName", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.reponame"),
            @Param(name = "configList.branchFilter", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.branchfilter", type = ApiParamType.STRING),
            @Param(name = "configList.callbackUrl", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.callbackurl", type = ApiParamType.JSONARRAY),
            @Param(name = "configList.username", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.username", type = ApiParamType.STRING),
            @Param(name = "configList.password", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.password", type = ApiParamType.STRING),
            @Param(name = "configList.action", desc = "nmdac.batchupdategitlabhookapi.input.param.desc.configlist.action", type = ApiParamType.ENUM, rule = "insert,delete"),
    })
    @Description(desc = "nmdac.batchupdategitlabhookapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray array = new JSONArray();
        String globalRepoServerAddress = paramObj.getString("repoServerAddress");
        JSONArray globalCallbackUrl = paramObj.getJSONArray("callbackUrl");
        String globalBranchFilter = paramObj.getString("branchFilter");
        String globalUsername = paramObj.getString("username");
        String globalPassword = paramObj.getString("password");
        String globalAction = paramObj.getString("action");
        String encPassword = null;
        if (StringUtils.isNotBlank(globalPassword)) {
            encPassword = RC4Util.encrypt(globalPassword);
        }
        JSONArray configList = paramObj.getJSONArray("configList");
        for (int i = 0; i < configList.size(); i++) {
            JSONObject config = configList.getJSONObject(i);
            JSONArray callbackUrl = config.getJSONArray("callbackUrl");
            String repoName = config.getString("repoName");
            String username = config.getString("username");
            String password = config.getString("password");
            String action = config.getString("action");
            if (StringUtils.isBlank(repoName)) {
                throw new DeployGitlabHookLessRepoNameException(i + 1);
            }
            if (CollectionUtils.isEmpty(callbackUrl)) {
                config.put("callbackUrl", globalCallbackUrl);
                if (CollectionUtils.isEmpty(globalCallbackUrl)) {
                    throw new DeployGitlabHookRepoNameLessParamException(repoName, "callbackUrl");
                }
            }
            if (StringUtils.isBlank(username)) {
                config.put("username", globalUsername);
                if (StringUtils.isBlank(globalUsername)) {
                    throw new DeployGitlabHookRepoNameLessParamException(repoName, "username");
                }
            }
            if (StringUtils.isBlank(password)) {
                config.put("password", encPassword);
                if (StringUtils.isBlank(encPassword)) {
                    throw new DeployGitlabHookRepoNameLessParamException(repoName, "password");
                }
            } else {
                config.put("password", RC4Util.encrypt(password));
            }
            if (StringUtils.isBlank(config.getString("branchFilter"))) {
                config.put("branchFilter", globalBranchFilter);
            }
            if (StringUtils.isBlank(action)) {
                config.put("action", globalAction);
                action = globalAction;
                if (StringUtils.isBlank(globalAction)) {
                    throw new DeployGitlabHookRepoNameLessParamException(repoName, "action");
                }
            }
            if (!"insert".equals(action) && !"delete".equals(action)) {
                throw new DeployGitlabHookRepoNameUnknownActionException(repoName , action);
            }
            config.put("repoServerAddress", globalRepoServerAddress);
            config.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
            config.put("event", DeployCiRepoEvent.POSTRECEIVE.getValue());
            array.add(config);
        }
        String url = paramObj.getString("runnerUrl") + "/autoexecrunner/api/rest/deploy/ci/gitlabwebhook/batchupdate";
        JSONObject param = new JSONObject();
        param.put("configList", array);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            throw new ApiRuntimeException(error);
        }
        JSONObject resultJson = request.getResultJson();
        if (MapUtils.isNotEmpty(resultJson)) {
            return resultJson.getJSONArray("Return");
        }
        return null;
    }

}
