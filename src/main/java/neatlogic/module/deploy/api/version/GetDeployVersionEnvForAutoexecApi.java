package neatlogic.module.deploy.api.version;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionEnvNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
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

    @Resource
    AuthenticationInfoService authenticationInfoService;

    @Override
    public String getName() {
        return "nmdav.getdeployversionenvforautoexecapi.getname";
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
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "erm.cmdb.moduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "term.cmdb.envid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "proxyToUrl", desc = "term.deploy.proxytourl", help = "可选，如果有则表示去其他环境获取", rule = RegexUtils.CONNECT_URL, type = ApiParamType.REGEX),
    })
    @Description(desc = "nmdav.getdeployversionenvforautoexecapi.getname")
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
            AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(credentialUserUuid);
            String url = proxyToUrl + UserContext.get().getRequest().getRequestURI();
            UserContext.init(credentialUser, authenticationInfo, "+8:00");
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url)
                    .setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .sendRequest();
            if (httpRequestUtil != null) {
                int responseCode = httpRequestUtil.getResponseCode();
                String error = httpRequestUtil.getError();
                if (StringUtils.isNotBlank(error)) {
                    if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
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
