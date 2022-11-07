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
            @Param(name = "configList", desc = "hook配置列表", type = ApiParamType.JSONARRAY, isRequired = true),
            @Param(name = "configList.repoServerAddress", desc = "仓库服务器地址"),
            @Param(name = "configList.repoName", desc = "仓库名称"),
            @Param(name = "configList.branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "configList.callbackUrl", desc = "回调url", type = ApiParamType.STRING),
            @Param(name = "configList.username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "configList.password", desc = "gitlab密码", type = ApiParamType.STRING),
            @Param(name = "configList.action", desc = "动作类型", type = ApiParamType.ENUM, rule = "insert,delete"),
    })
    @Description(desc = "批量更新gitlab webhook")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray array = new JSONArray();
        JSONArray configList = paramObj.getJSONArray("configList");
        for (int i = 0; i < configList.size(); i++) {
            JSONObject config = configList.getJSONObject(i);
            String callbackUrl = config.getString("callbackUrl");
            String repoServerAddress = config.getString("repoServerAddress");
            String repoName = config.getString("repoName");
            String username = config.getString("username");
            String password = config.getString("password");
            String action = config.getString("action");
            if (StringUtils.isBlank(repoServerAddress)) {
                throw new ApiRuntimeException("第：" + (i + 1) + "个缺少repoServerAddress");
            }
            if (StringUtils.isBlank(repoName)) {
                throw new ApiRuntimeException(repoServerAddress + "缺少repoName");
            }
            if (StringUtils.isBlank(username)) {
                throw new ApiRuntimeException(repoServerAddress + "/" + repoName + "缺少username");
            }
            if (StringUtils.isBlank(password)) {
                throw new ApiRuntimeException(repoServerAddress + "/" + repoName + "缺少password");
            }
            if (StringUtils.isBlank(action)) {
                throw new ApiRuntimeException(repoServerAddress + "/" + repoName + "缺少action");
            }
            if (!"insert".equals(action) && !"delete".equals(action)) {
                throw new ApiRuntimeException(repoServerAddress + "/" + repoName + "未知的action：" + action);
            }
            if ("insert".equals(action) && StringUtils.isBlank(callbackUrl)) {
                throw new ApiRuntimeException(repoServerAddress + "/" + repoName + "缺少callbackUrl");
            }
            config.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
            config.put("event", DeployCiRepoEvent.POSTRECEIVE.getValue());
            config.put(password, RC4Util.encrypt(password));
            array.add(config);
        }
        String url = paramObj.getString("runnerUrl") + "/api/rest/deploy/ci/gitlabwebhook/batchupdate";
        JSONObject param = new JSONObject();
        param.put("configList", array);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        JSONObject resultJson = request.getResultJson();
        if (MapUtils.isNotEmpty(resultJson)) {
            return resultJson.getJSONArray("Message");
        }
        return null;
    }

}
