/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
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
public class CreateDeployJobApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(CreateDeployJobApi.class);
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
        convertParam(jsonObj);
        deployJobService.initDeployParam(jsonObj);
        JSONArray moduleArray = jsonObj.getJSONArray("moduleList");
        BatchRunner<Object> runner = new BatchRunner<>();
        runner.execute(moduleArray, 3, module -> {
            JSONObject moduleJson = JSONObject.parseObject(module.toString());
            if (MapUtils.isNotEmpty(moduleJson)) {
                try {
                    deployJobService.convertModule(jsonObj, moduleJson);
                    if (jsonObj.containsKey("triggerType")) {
                        result.add(deployJobService.createScheduleJob(jsonObj));
                    } else {
                        result.add(deployJobService.createJob(jsonObj));
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
        }, "DEPLOY-JOB-CREATE");
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create";
    }

    /**
     * 兼容波哥 autoexec 入参
     *
     * @param jsonObj 入参
     */
    private void convertParam(JSONObject jsonObj) {
        if (jsonObj.containsKey("sysName")) {
            jsonObj.put("appSystemName", jsonObj.getString("sysName"));
        }
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
