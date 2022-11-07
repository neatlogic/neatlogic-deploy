package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployCiGitlabAuthMode;
import codedriver.framework.deploy.constvalue.DeployCiRepoEvent;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
        return "批量更新gitlab webhook";
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
            @Param(name = "runnerUrl", desc = "runner url", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoServerAddress", desc = "仓库服务器地址", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "password", desc = "gitlab密码", type = ApiParamType.STRING),
            @Param(name = "configList", desc = "hook配置列表", type = ApiParamType.JSONARRAY, isRequired = true),
            @Param(name = "configList.repoServerAddress", desc = "仓库服务器地址"),
            @Param(name = "configList.repoName", desc = "仓库名称"),
            @Param(name = "configList.branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "configList.callbackUrl", desc = "回调url", type = ApiParamType.JSONARRAY),
            @Param(name = "configList.username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "configList.password", desc = "gitlab密码", type = ApiParamType.STRING),
            @Param(name = "configList.action", desc = "动作类型", type = ApiParamType.ENUM, rule = "insert,delete"),
    })
    @Description(desc = "批量更新gitlab webhook")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray array = new JSONArray();
        String globalRepoServerAddress = paramObj.getString("repoServerAddress");
        String globalBranchFilter = paramObj.getString("branchFilter");
        String globalUsername = paramObj.getString("username");
        String globalPassword = paramObj.getString("password");
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
                throw new ApiRuntimeException("第：" + (i + 1) + "个缺少repoName");
            }
            if (CollectionUtils.isEmpty(callbackUrl)) {
                throw new ApiRuntimeException(repoName + "缺少callbackUrl");
            }
            if (StringUtils.isBlank(username)) {
                config.put("username", globalUsername);
                if (StringUtils.isBlank(globalUsername)) {
                    throw new ApiRuntimeException(repoName + "缺少username");
                }
            }
            if (StringUtils.isBlank(password)) {
                config.put("password", encPassword);
                if (StringUtils.isBlank(encPassword)) {
                    throw new ApiRuntimeException(repoName + "缺少password");
                }
            } else {
                config.put("password", RC4Util.encrypt(password));
            }
            if (StringUtils.isBlank(config.getString("branchFilter"))) {
                config.put("branchFilter", globalBranchFilter);
            }
            if (StringUtils.isBlank(action)) {
                throw new ApiRuntimeException(repoName + "缺少action");
            }
            if (!"insert".equals(action) && !"delete".equals(action)) {
                throw new ApiRuntimeException(repoName + "未知的action：" + action);
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
        JSONObject resultJson = request.getResultJson();
        if (MapUtils.isNotEmpty(resultJson)) {
            return resultJson.getJSONArray("Return");
        }
        return null;
    }

}
