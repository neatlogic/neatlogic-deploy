package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.SystemUser;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionEnvNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@neatlogic.framework.restful.annotation.SystemUser("autoexec")
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class GetDeployVersionEnvForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

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
            @Param(name = "sysName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.sysname"),
            @Param(name = "moduleName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.modulename"),
            @Param(name = "envName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.envname"),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "proxyToUrl", desc = "term.deploy.proxytourl", help = "可选，如果有则表示去其他环境获取", rule = RegexUtils.CONNECT_URL, type = ApiParamType.REGEX),
    })
    @Description(desc = "nmdav.getdeployversionenvforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        String proxyToUrl = paramObj.getString("proxyToUrl");
        if (StringUtils.isBlank(proxyToUrl)) {
            String sysName = paramObj.getString("sysName");
            String moduleName = paramObj.getString("moduleName");
            String version = paramObj.getString("version");
            String envName = paramObj.getString("envName");
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(sysName);
            if (appSystem == null) {
                throw new AppSystemNotFoundException(sysName);
            }
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(moduleName);
            if (appModule == null) {
                throw new AppModuleNotFoundException(moduleName);
            }
            Long sysId = appSystem.getId();
            Long moduleId = appModule.getId();
            paramObj.put("sysId", sysId);
            paramObj.put("moduleId", moduleId);
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
            if (versionVo == null) {
                throw new DeployVersionNotFoundException(version);
            }
            //env status
            ResourceVo env = resourceCrossoverMapper.getAppEnvByName(envName);
            if (env == null) {
                throw new AppEnvNotFoundException(envName);
            }
            DeployVersionEnvVo versionEnvVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(), env.getId());
            if (versionEnvVo == null) {
                throw new DeployVersionEnvNotFoundException(sysName, moduleName, envName, version);
            }

            result.put("version", versionVo.getVersion());
            result.put("buildNo", versionEnvVo.getBuildNo());
            result.put("isMirror", versionEnvVo.getIsMirror());
            result.put("status", versionEnvVo.getStatus());
        } else {
//            String credentialUserUuid = deployVersionMapper.getDeployVersionAppbuildCredentialByProxyToUrl(proxyToUrl);
//            UserVo credentialUser = userMapper.getUserByUuid(credentialUserUuid);
//            if (credentialUser == null) {
//                throw new DeployVersionRedirectUrlCredentialUserNotFoundException(credentialUserUuid);
//            }
//            AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(credentialUserUuid);
            //改为系统虚拟用户
            String url = proxyToUrl + UserContext.get().getRequest().getRequestURI();
            UserContext.init(SystemUser.AUTOEXEC);
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.AUTOEXEC.getUserVo()).getCc());
            //到别的环境去验证
            paramObj.remove("proxyToUrl");
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
