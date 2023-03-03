/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.job;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Transactional
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateMultiDeployJobApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(CreateMultiDeployJobApi.class);
    @Resource
    DeployVersionMapper deployVersionMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    private DeployJobService deployJobService;

    @Override
    public String getName() {
        return "创建并执行发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appSystemName", type = ApiParamType.STRING, desc = "应用系统名，如果入参也有appSystemId，则会以appSystemName为准"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用系统名，如果入参也有appSystemId，则会以sysName为准"),
            @Param(name = "moduleList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "模块列表"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境id，如果入参也有envId，则会以envName为准"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源 itsm|human|deploy   ITSM|人工发起的等，不传默认是发布发起的"),
            @Param(name = "roundCount", type = ApiParamType.LONG, isRequired = true, desc = "分组数 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划时间"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "auto,manual", desc = "触发方式"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "指定执行用户"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id"),
            @Param(name = "proxyToUrl", type = ApiParamType.STRING, desc = "不从当前环境runner下载,则需要传跳转url，即协议+IP地址（域名）+端口号")

    })
    @Description(desc = "创建并执行发布作业接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        if (StringUtils.isNotBlank(jsonObj.getString("proxyToUrl"))) {
            proxyToUrl(jsonObj);
        }
        JSONArray result = new JSONArray();
        DeployJobVo deployJobParam = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        BatchRunner<DeployJobModuleVo> runner = new BatchRunner<>();
        runner.execute(deployJobParam.getModuleList(), 3, module -> {
            if (module != null) {
                //防止后续多个作业用的同一个作业
                DeployJobVo deployJob = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
                try {
                    if (jsonObj.containsKey("triggerType")) {
                        result.add(deployJobService.createJobAndSchedule(deployJob, module));
                    } else {
                        result.add(deployJobService.createJobAndFire(deployJob, module));
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("appSystemName", jsonObj.getString("appSystemName"));
                    resultJson.put("appModuleName", jsonObj.getString("appModuleName"));
                    resultJson.put("errorMsg", ex.getMessage());
                    result.add(resultJson);
                }

            }
        }, "DEPLOY-JOB-MULTI-CREATE");
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/multi/create";
    }

    private void proxyToUrl(JSONObject jsonObj) throws Exception {
        String proxyToUrl = jsonObj.getString("proxyToUrl");
        String credentialUserUuid = deployVersionMapper.getDeployVersionAppbuildCredentialByProxyToUrl(proxyToUrl);
        UserVo credentialUser = userMapper.getUserByUuid(credentialUserUuid);
        if (credentialUser == null) {
            throw new DeployVersionRedirectUrlCredentialUserNotFoundException(credentialUserUuid);
        }
        HttpServletRequest request = UserContext.get().getRequest();
        HttpServletResponse response = UserContext.get().getResponse();
        UserContext.init(credentialUser, "+8:00", request, response);
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(credentialUser).getCc());
        String requestURI = request.getRequestURI();
        String url = proxyToUrl + requestURI;
        HttpRequestUtil httpRequestUtil = null;
        httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream())
                .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN)
                .addHeader("User-Agent", request.getHeader("User-Agent"))
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
        }
    }
}
