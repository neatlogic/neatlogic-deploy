/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.file.transfer;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.deploy.exception.DownloadFileFailedException;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.runner.RunnerNotFoundInGroupException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadDeployAppBuildApi extends PrivateBinaryStreamApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(DownloadDeployAppBuildApi.class);
    @Resource
    RunnerMapper runnerMapper;
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

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
            @Param(name = "appSystemName", type = ApiParamType.STRING, desc = "系统名称"),
            @Param(name = "appModuleName", type = ApiParamType.STRING, desc = "模块名称"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, isRequired = true, desc = "build no"),
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "版本名称"),
            @Param(name = "subDirs", type = ApiParamType.STRING, desc = "subDirs"),
            @Param(name = "runnerId", type = ApiParamType.LONG, isRequired = true, desc = "执行器id,-1则根据应用和模块对应的group随意选择一个runner"),
            @Param(name = "isCurrentEnvRunner", type = ApiParamType.ENUM, rule = "0,1", desc = "是否从当前环境runner下载,1：是，0：否。默认是"),
    })
    @Output({
    })
    @Description(desc = "下载 appbuild接口")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer isCurrentEnvRunner = jsonObj.getInteger("isCurrentEnvRunner");
        if (isCurrentEnvRunner == null || isCurrentEnvRunner == 1) {
            downloadFromCurrentEnvRunner(jsonObj, request, response);
        } else {
            downloadFromOtherEnv(jsonObj, request, response);
        }
        return null;
    }


    private void downloadFromOtherEnv(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String otherUrl = "http://localhost:8080";//TODO 获取跳转环境的host和认证
        String requestURI = request.getRequestURI();
        jsonObj.put("isCurrentEnvRunner",1);
        jsonObj.put("runnerId",-1);
        String url = otherUrl + requestURI;
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
        if(runnerId == -1){
            Long appSystemId = jsonObj.getLong("appSystemId");
            Long appModuleId = jsonObj.getLong("appModuleId");
            RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId,appModuleId);
            if(runnerGroupVo == null){
                throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystemId.toString(),appModuleId.toString());
            }else{
                List<RunnerMapVo> runnerMapVoList = runnerGroupVo.getRunnerMapList();
                if(CollectionUtils.isEmpty(runnerMapVoList)){
                    throw new RunnerNotFoundInGroupException(runnerGroupVo.getId());
                }
                runnerUrl = runnerMapVoList.get(0).getUrl();
            }
        }else {
            RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(runnerId);
            if (runnerMapVo == null) {
                throw new RunnerNotFoundByRunnerMapIdException(runnerId);
            }
            runnerUrl =runnerMapVo.getUrl();
        }
        // 对buildNo目录上锁
        IGlobalLockHandler handler = handler = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue());
        JSONObject lockJson = new JSONObject();
        String path = File.separator + jsonObj.getString("appSystemId") + File.separator + jsonObj.getString("appModuleId") + File.separator + "artifact" + File.separator + jsonObj.getString("version") + File.separator + "build" + File.separator + jsonObj.getString("buildNo");
        lockJson.put("runnerUrl",runnerUrl );
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
