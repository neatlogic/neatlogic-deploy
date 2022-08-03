/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appbuild;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.LicenseMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.BuildNoStatus;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.core.LicenseInvalidException;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.runner.RunnerNotFoundInGroupException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadDeployAppBuildApi extends PrivateBinaryStreamApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(DownloadDeployAppBuildApi.class);
    @Resource
    RunnerMapper runnerMapper;
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;
    @Resource
    DeployVersionMapper deployVersionMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    LicenseMapper licenseMapper;


    @Override
    public String getName() {
        return "下载 appbuild";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/appbuild/download";
    }

    @Input({
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "版本名称"),
            @Param(name = "sysName", type = ApiParamType.STRING, isRequired = true, desc = "系统简称"),
            @Param(name = "moduleName", type = ApiParamType.STRING, isRequired = true, desc = "模块简称"),
            @Param(name = "envName", type = ApiParamType.STRING, isRequired = true, desc = "环境简称"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, desc = "build no, 如果是空的，那么就要到版本对应的envName找到buildNo"),
            @Param(name = "subDirs", type = ApiParamType.JSONARRAY, desc = "subDirs 子目录"),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "执行器id,-1则根据应用和模块对应的group随意选择一个runner"),
            @Param(name = "proxyToUrl", type = ApiParamType.STRING, desc = "不从当前环境runner下载,则需要传跳转url，即协议+IP地址（域名）+端口号，不传默认本地环境"),
            @Param(name = "proxyTenantLicense", type = ApiParamType.STRING, desc = "proxy license"),
    })
    @Output({
    })
    @Description(desc = "下载 appbuild接口")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String proxyTenantLicense = jsonObj.getString("proxyTenantLicense");
        //有license 说明是proxy过来的
        if (StringUtils.isNotBlank(proxyTenantLicense)) {
            getResponseHeader(jsonObj, response);
            String tenantUuid = TenantContext.get().getTenantUuid();
            TenantContext.get().setUseDefaultDatasource(true);
            String tenantLicense = licenseMapper.getTenantLicenseByTenantUuid(tenantUuid);
            TenantContext.get().setUseDefaultDatasource(false);
            if (!Objects.equals(proxyTenantLicense, tenantLicense)) {
                downloadFromCurrentEnv(jsonObj, request, response);
            }
        } else {
            String proxyToUrl = jsonObj.getString("proxyToUrl");
            if (StringUtils.isNotBlank(proxyToUrl)) {
                downloadFromOtherEnv(jsonObj, request, response);
            } else {
                //无需下载appbuild
                getResponseHeader(jsonObj, response);
            }
        }
        return null;
    }

    /**
     * 补充当前环境header信息
     *
     * @param jsonObj  入参
     * @param response 响应
     */
    private void getResponseHeader(JSONObject jsonObj, HttpServletResponse response) {
        String sysName = jsonObj.getString("sysName");
        String moduleName = jsonObj.getString("moduleName");
        String version = jsonObj.getString("version");
        String envName = jsonObj.getString("envName");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(sysName, TenantContext.get().getDataDbName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(sysName);
        }
        ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(moduleName, TenantContext.get().getDataDbName());
        if (appModule == null) {
            throw new AppModuleNotFoundException(moduleName);
        }
        Long sysId = appSystem.getId();
        Long moduleId = appModule.getId();
        jsonObj.put("sysId", sysId);
        jsonObj.put("moduleId", moduleId);
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        //env status
        ResourceVo env = resourceCrossoverMapper.getAppEnvByName(envName, TenantContext.get().getDataDbName());
        if (env == null) {
            throw new AppEnvNotFoundException(envName);
        }
        DeployVersionEnvVo versionEnvVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(), env.getId());
        if (versionEnvVo == null) {
            throw new DeployVersionEnvNotFoundException(sysName, moduleName, envName, version);
        }
        response.setHeader("Build-Env-Status", versionEnvVo.getStatus());
        if (versionEnvVo.getIsMirror() != null) {
            response.setHeader("isMirror", versionEnvVo.getIsMirror().toString());
        }
        //判断buildNo状态，如果是released 则返回下载流，否则返回错误json信息
        Integer buildNo = jsonObj.getInteger("buildNo");
        if (buildNo == null) {
            buildNo = versionEnvVo.getBuildNo();
        }
        if (buildNo == null) {
            throw new DeployBuildNoNotFoundException(appSystem.getName(), appModule.getName(), version);
        }
        DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        if (buildNoVo == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        response.setHeader("Build-No", buildNo.toString());
        response.setHeader("Build-Status", buildNoVo.getStatus());
        jsonObj.put("buildNo", buildNo);

        if (!Objects.equals(buildNoVo.getStatus(), BuildNoStatus.RELEASED.getValue())) {
            throw new DeployVersionBuildNoStatusIsNotReleasedException(sysName, moduleName, envName, version, buildNo, buildNoVo.getStatus());
        }
    }


    /**
     * 从别的环境下载
     *
     * @param jsonObj  入参
     * @param request  请求
     * @param response 响应
     * @throws Exception 异常
     */
    private void downloadFromOtherEnv(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String proxyToUrl = jsonObj.getString("proxyToUrl");
        String credentialUserUuid = deployVersionMapper.getDeployVersionAppbuildCredentialByProxyToUrl(proxyToUrl);
        UserVo credentialUser = userMapper.getUserByUuid(credentialUserUuid);
        if (credentialUser == null) {
            throw new DeployVersionRedirectUrlCredentialUserNotFoundException(credentialUserUuid);
        }
        UserContext.init(credentialUser, "+8:00", request, response);
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
        String requestURI = request.getRequestURI();
        jsonObj.put("isCurrentEnvRunner", 1);
        jsonObj.put("runnerId", -1);
        String url = proxyToUrl + requestURI;
        //获取license唯一标识
        String tenantUuid = TenantContext.get().getTenantUuid();
        TenantContext.get().setUseDefaultDatasource(true);
        String tenantLicense = licenseMapper.getTenantLicenseByTenantUuid(tenantUuid);
        TenantContext.get().setUseDefaultDatasource(false);
        if (StringUtils.isBlank(tenantLicense)) {
            throw new LicenseInvalidException(TenantContext.get().getTenantUuid(), "license is ", tenantLicense);
        }
        jsonObj.put("proxyTenantLicense", tenantLicense);
        HttpRequestUtil httpRequestUtil = null;
        try {
            httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .addHeader("User-Agent", request.getHeader("User-Agent"))
                    .setResponseHeaders(Arrays.asList("Build-No","Build-Status","Build-Env-Status","isMirror","Test"))
                    .sendRequest();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if (httpRequestUtil != null) {
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == 520) {
                    throw new DownloadFileFailedException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new DownloadFileFailedException(error);
                }
            }
        }
    }

    /**
     * 从当前环境runner下载
     *
     * @param jsonObj  入参
     * @param request  请求
     * @param response 相应
     */
    private void downloadFromCurrentEnv(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) {
        //获取对应的sysId、moduleId
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(jsonObj.getString("sysName"), TenantContext.get().getDataDbName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(jsonObj.getString("sysName"));
        }
        ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(jsonObj.getString("moduleName"), TenantContext.get().getDataDbName());
        if (appModule == null) {
            throw new AppModuleNotFoundException(jsonObj.getString("moduleName"));
        }
        String runnerUrl;
        Long runnerId = jsonObj.getLong("runnerId");
        //兼容跳转web需要根据app｜module 获取 对应runner组的任意runner
        if (runnerId == -1) {
            Long sysId = appSystem.getId();
            Long moduleId = appModule.getId();
            jsonObj.put("sysId", sysId);
            jsonObj.put("moduleId", moduleId);
            RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(sysId, moduleId);
            if (runnerGroupVo == null) {
                throw new DeployAppConfigModuleRunnerGroupNotFoundException(sysId.toString(), moduleId.toString());
            } else {
                List<RunnerMapVo> runnerMapVoList = runnerGroupVo.getRunnerMapList();
                if (CollectionUtils.isEmpty(runnerMapVoList)) {
                    throw new RunnerNotFoundInGroupException(runnerGroupVo.getId());
                }
                runnerUrl = runnerMapVoList.get(0).getUrl();
            }
        } else {
            RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(runnerId);
            if (runnerMapVo == null) {
                throw new RunnerNotFoundByRunnerMapIdException(runnerId);
            }
            runnerUrl = runnerMapVo.getUrl();
        }
        // 对buildNo目录上锁
        IGlobalLockHandler handler = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue());
        JSONObject lockJson = new JSONObject();
        String path = File.separator + jsonObj.getString("sysId") + File.separator + jsonObj.getString("moduleId") + File.separator + "artifact" + File.separator + jsonObj.getString("version") + File.separator + "build" + File.separator + jsonObj.getString("buildNo");
        lockJson.put("runnerUrl", runnerUrl);
        lockJson.put("path", path);
        JSONObject lock = handler.getLock(lockJson);
        Long lockId = lock.getLong("lockId");
        if (lockId == null) {
            throw new ApiRuntimeException(lock.getString("message"));
        }
        HttpRequestUtil httpRequestUtil = null;
        String url = String.format("%s/api/binary/deploy/appbuild/download", runnerUrl);
        try {
            httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .addHeader("User-Agent", request.getHeader("User-Agent")).sendRequest();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            // 释放锁
            handler.cancelLock(lockId, null);
        }
        if (httpRequestUtil != null) {
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == 520) {
                    throw new DownloadFileFailedException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new DownloadFileFailedException(error);
                }
            }
        }
    }

}
