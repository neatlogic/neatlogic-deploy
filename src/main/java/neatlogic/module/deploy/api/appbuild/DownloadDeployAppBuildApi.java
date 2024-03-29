/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.appbuild;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.BuildNoStatus;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.exception.runner.RunnerNotFoundInGroupException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.globallock.core.GlobalLockHandlerFactory;
import neatlogic.framework.globallock.core.IGlobalLockHandler;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.framework.store.mysql.DatasourceManager;
import neatlogic.framework.store.mysql.NeatLogicBasicDataSource;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
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
    AuthenticationInfoService authenticationInfoService;

    @Override
    public String getName() {
        return "nmdaa.downloaddeployappbuildapi.getname";
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
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "common.versionname"),
            @Param(name = "sysName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.sysname"),
            @Param(name = "moduleName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.modulename"),
            @Param(name = "envName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.envname"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, desc = "build no", help = "如果是空的，那么就要到版本对应的envName找到buildNo"),
            @Param(name = "subDirs", type = ApiParamType.JSONARRAY, desc = "nmdaa.downloaddeployappbuildapi.input.param.desc.subdirs"),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "term.deploy.runnerid", help = "-1则根据应用和模块对应的group随意选择一个runner"),
            @Param(name = "proxyToUrl", type = ApiParamType.STRING, desc = "term.deploy.proxytourl", help = "不从当前环境runner下载,则需要传跳转url，即协议+IP地址（域名）+端口号，不传默认本地环境"),
            @Param(name = "proxyTenantJdbcUrl", type = ApiParamType.STRING, desc = "proxy license"),
    })
    @Output({
    })
    @Description(desc = "nmdaa.downloaddeployappbuildapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String proxyTenantJdbcUrl = jsonObj.getString("proxyTenantJdbcUrl");
        //有jdbcUrl 说明是proxy过来的
        if (StringUtils.isNotBlank(proxyTenantJdbcUrl)) {
            getResponseHeader(jsonObj, response);
            String tenantUuid = TenantContext.get().getTenantUuid();
            NeatLogicBasicDataSource tenantDbSource = DatasourceManager.getDatasource(tenantUuid);
            if (!Objects.equals(proxyTenantJdbcUrl, tenantDbSource.getJdbcUrl())) {
                response.setHeader("Build-Local", "false");
                downloadFromCurrentEnv(jsonObj, request, response);
            } else {
                response.setHeader("Build-Local", "true");
            }
        } else {
            String proxyToUrl = jsonObj.getString("proxyToUrl");
            if (StringUtils.isNotBlank(proxyToUrl)) {
                downloadFromOtherEnv(jsonObj, request, response);
            } else {
                //无需下载appbuild
                getResponseHeader(jsonObj, response);
                response.setHeader("Build-Local", "true");
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
        jsonObj.put("sysId", sysId);
        jsonObj.put("moduleId", moduleId);
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
        AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(credentialUserUuid);
        UserContext.init(credentialUser, authenticationInfo, "+8:00", request, response);
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
        String requestURI = request.getRequestURI();
        jsonObj.put("isCurrentEnvRunner", 1);
        jsonObj.put("runnerId", -1);
        String url = proxyToUrl + requestURI;
        //获取license唯一标识
        String tenantUuid = TenantContext.get().getTenantUuid();
        NeatLogicBasicDataSource tenantDbSource = DatasourceManager.getDatasource(tenantUuid);
        jsonObj.put("proxyTenantJdbcUrl", tenantDbSource.getJdbcUrl());
        HttpRequestUtil httpRequestUtil = null;
        try {
            httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .addHeader("User-Agent", request.getHeader("User-Agent"))
                    .setResponseHeaders(Arrays.asList("Build-No", "Build-Status", "Build-Env-Status", "isMirror", "Build-Local"))
                    .sendRequest();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if (httpRequestUtil != null) {
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
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
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(jsonObj.getString("sysName"));
        if (appSystem == null) {
            throw new AppSystemNotFoundException(jsonObj.getString("sysName"));
        }
        ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(jsonObj.getString("moduleName"));
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
            handler.unLock(lockId, null);
        }
        if (httpRequestUtil != null) {
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
                    throw new DownloadFileFailedException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new DownloadFileFailedException(error);
                }
            }
        }
    }

}
