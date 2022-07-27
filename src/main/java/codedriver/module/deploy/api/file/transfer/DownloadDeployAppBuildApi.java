/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.file.transfer;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import codedriver.framework.deploy.exception.DownloadFileFailedException;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.runner.RunnerNotFoundInGroupException;
import codedriver.framework.exception.type.ParamIrregularException;
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
            @Param(name = "sysId", type = ApiParamType.LONG, isRequired = true, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, isRequired = true, desc = "build no"),
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "版本名称"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "系统名称"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名称"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "subDirs", type = ApiParamType.STRING, desc = "subDirs"),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "执行器id,-1则根据应用和模块对应的group随意选择一个runner"),
            @Param(name = "isCurrentEnvRunner", type = ApiParamType.ENUM, rule = "0,1", desc = "是否从当前环境runner下载,1：是，0：否。默认否"),
            @Param(name = "redirectUrl", type = ApiParamType.STRING, desc = "不从当前环境runner下载,则需要传跳转url，即协议+IP地址（域名）+端口号")
    })
    @Output({
    })
    @Description(desc = "下载 appbuild接口")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer isCurrentEnvRunner = jsonObj.getInteger("isCurrentEnvRunner");
        if (Objects.equals(isCurrentEnvRunner, 1)) {
            downloadFromCurrentEnvRunner(jsonObj, request, response);
        } else {
            downloadFromOtherEnv(jsonObj, request, response);
        }
        return null;
    }


    private void downloadFromOtherEnv(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String redirectUrl = jsonObj.getString("redirectUrl");
        if (StringUtils.isBlank(redirectUrl)) {
            throw new ParamIrregularException("redirectUrl");
        }
        String credentialUserUuid = deployVersionMapper.getDeployVersionAppbuildCredentialByRedirectUrl(redirectUrl);
        UserVo credentialUser = userMapper.getUserByUuid(credentialUserUuid);
        if(credentialUser == null){
            throw new DeployVersionRedirectUrlCredentialUserNotFoundException(credentialUserUuid);
        }
        UserContext.init(credentialUser, "+8:00", request, response);
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
        String requestURI = request.getRequestURI();
        jsonObj.put("isCurrentEnvRunner", 1);
        jsonObj.put("runnerId", -1);
        String url = redirectUrl + requestURI;
        HttpRequestUtil httpRequestUtil = null;
        try {
            httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                    .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                    .addHeader("User-Agent", request.getHeader("User-Agent"))
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
    private void downloadFromCurrentEnvRunner(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) {
        String runnerUrl;
        Long runnerId = jsonObj.getLong("runnerId");
        //兼容跳转web需要根据app｜module 获取 对应runner组的任意runner
        if (runnerId == -1) {
            Long sysId = jsonObj.getLong("sysId");
            Long moduleId = jsonObj.getLong("moduleId");
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
