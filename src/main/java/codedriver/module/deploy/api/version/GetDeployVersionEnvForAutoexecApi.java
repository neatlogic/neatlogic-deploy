package codedriver.module.deploy.api.version;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.RegexUtils;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class GetDeployVersionEnvForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "获取发布版本环境信息";
    }

    @Override
    public String getToken() {
        return "deploy/version/env/get/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用系统id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "环境id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "proxyToUrl", desc = "地址（可选，如果有则表示去其他环境获取）", rule = RegexUtils.CONNECT_URL, type = ApiParamType.REGEX),
    })
    @Description(desc = "获取发布版本环境信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        String proxyToUrl = paramObj.getString("proxyToUrl");
        if (StringUtils.isBlank(proxyToUrl)) {
            Long sysId = paramObj.getLong("sysId");
            Long moduleId = paramObj.getLong("moduleId");
            Long envId = paramObj.getLong("envId");
            String version = paramObj.getString("version");
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
            if (versionVo == null) {
                throw new DeployVersionNotFoundException(version);
            }
            DeployVersionEnvVo envVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(), envId);
            if (envVo == null) {
                throw new DeployVersionEnvNotFoundException(versionVo.getVersion(), envId);
            }
            result.put("version", versionVo.getVersion());
            result.put("buildNo", envVo.getBuildNo());
            result.put("isMirror", envVo.getIsMirror());
            result.put("status", envVo.getStatus());
        } else {
            String credentialUserUuid = deployVersionMapper.getDeployVersionAppbuildCredentialByProxyToUrl(proxyToUrl);
            UserVo credentialUser = userMapper.getUserByUuid(credentialUserUuid);
            if (credentialUser == null) {
                throw new DeployVersionRedirectUrlCredentialUserNotFoundException(credentialUserUuid);
            }
            String url = proxyToUrl + UserContext.get().getRequest().getRequestURI();
            UserContext.init(credentialUser, "+8:00");
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url)
                    .setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .sendRequest();
            if (httpRequestUtil != null) {
                int responseCode = httpRequestUtil.getResponseCode();
                String error = httpRequestUtil.getError();
                if (StringUtils.isNotBlank(error)) {
                    if (responseCode == 520) {
                        throw new ApiRuntimeException(JSONObject.parseObject(error).getString("Message"));
                    } else {
                        throw new ApiRuntimeException(error);
                    }
                }
                JSONObject resultJson = httpRequestUtil.getResultJson();
                result = resultJson.getJSONObject("Return");
            }
        }
        return result;
    }

}
