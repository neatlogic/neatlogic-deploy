/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.version.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionResourceHasBeenLockedException;
import codedriver.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import codedriver.framework.deploy.exception.DownloadFileFailedException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployVersionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadFileApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(DownloadFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getToken() {
        return "deploy/version/resource/file/download";
    }

    @Override
    public String getName() {
        return "下载文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo(当resourceType为[mirror*|workspace]时不需要)", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID(当resourceType为[build*|workspace]时不需要)", type = ApiParamType.LONG),
            @Param(name = "resourceType", member = DeployResourceType.class, desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true),
            @Param(name = "isPack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否打包")
    })
    @Output({})
    @Description(desc = "下载文件(若选择打包下载，下载的文件为压缩包)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String path = paramObj.getString("path");
        Integer isPack = paramObj.getInteger("isPack");
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        Long appSystemId = version.getAppSystemId();
        Long appModuleId = version.getAppModuleId();
        String runnerUrl;
        String url;
        String fullPath;
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        if (!DeployResourceType.WORKSPACE.equals(resourceType)) {
            String envName = deployVersionService.getEnvName(version.getVersion(), envId);
            runnerUrl = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            fullPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, path);
        } else {
            runnerUrl = deployVersionService.getWorkspaceRunnerUrl(version);
            fullPath = deployVersionService.getWorkspaceResourceFullPath(appSystemId, appModuleId, path);
        }
        url = runnerUrl + "api/binary/file/download";
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", fullPath);
        paramJson.put("isPack", isPack);

        Long lockId = null;
        IGlobalLockHandler handler = null;
        if (Objects.equals(isPack, 1)) {
            // 对HOME目录上锁
            handler = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue());
            JSONObject lockJson = new JSONObject();
            lockJson.put("runnerUrl", runnerUrl);
            lockJson.put("path", "/".equals(path) ? fullPath.substring(0, fullPath.length() - 1) : fullPath.replace(path, ""));
            JSONObject lock = handler.getLock(lockJson);
            lockId = lock.getLong("lockId");
            if (lockId == null) {
                throw new DeployVersionResourceHasBeenLockedException();
            }
            if (appSystemId != null && appModuleId != null) {
                String appSystemName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(appSystemId);
                String appModuleName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(appModuleId);
                if (StringUtils.isNotBlank(appSystemName) && StringUtils.isNotBlank(appModuleName)) {
                    paramJson.put("fileName", appSystemName + "-" + appModuleName);
                }
            }
        }
        HttpRequestUtil httpRequestUtil = null;
        try {
            httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                    .setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .addHeader("User-Agent", request.getHeader("User-Agent")).sendRequest();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            // 释放锁
            if (lockId != null) {
                handler.cancelLock(lockId, null);
            }
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
        return null;
    }
}
