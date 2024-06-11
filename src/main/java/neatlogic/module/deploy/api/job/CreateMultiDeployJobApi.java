/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.exception.DeployVersionRedirectUrlCredentialUserNotFoundException;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployJobService;
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
    @Resource
    AuthenticationInfoService authenticationInfoService;

    @Override
    public String getName() {
        return "nmdaj.createmultideployjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "term.autoexec.scenarioname", help = "如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid"),
            @Param(name = "appSystemAbbrName", type = ApiParamType.STRING, desc = "term.cmdb.sysname", help = "如果入参也有appSystemId，则会以appSystemName为准"),
            @Param(name = "moduleList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nfd.licensevo.entityfield.name.modules"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "term.cmdb.envname", help = "如果入参也有envId，则会以envName为准"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeparam"),
            @Param(name = "roundCount", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.roundcount"),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "auto,manual", desc = "common.triggertype"),
            @Param(name = "assignExecUser", type = ApiParamType.STRING, desc = "term.autoexec.assignexecuser"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "common.parentid"),
            @Param(name = "proxyToUrl", type = ApiParamType.STRING, desc = "term.deploy.proxytourl", help = "不从当前环境runner下载,则需要传跳转url，即协议+IP地址（域名）+端口号")

    })
    @Description(desc = "nmdaj.createmultideployjobapi.getname")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        if (StringUtils.isNotBlank(jsonObj.getString("proxyToUrl"))) {
            proxyToUrl(jsonObj);
        }
        JSONArray result = new JSONArray();
        DeployJobVo deployJobParam = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        if (StringUtils.isNotBlank(deployJobParam.getAppSystemAbbrName())) {
            AppSystemVo appSystem = iAppSystemMapper.getAppSystemByAbbrName(deployJobParam.getAppSystemAbbrName());
            if (appSystem == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemAbbrName());
            }
            deployJobParam.setAppSystemId(appSystem.getId());
            deployJobParam.setAppSystemName(appSystem.getName());
            deployJobParam.setAppSystemAbbrName(appSystem.getAbbrName());
        } else if (deployJobParam.getAppSystemId() != null) {
            AppSystemVo appSystem = iAppSystemMapper.getAppSystemById(deployJobParam.getAppSystemId());
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobParam.getAppSystemId()) == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemId());
            }
            deployJobParam.setAppSystemName(appSystem.getName());
            deployJobParam.setAppSystemAbbrName(appSystem.getAbbrName());
        } else {
            throw new ParamIrregularException("appSystemId | appSystemName");
        }
        Long invokeId = deployJobParam.getAppSystemId();
        BatchRunner<DeployJobModuleVo> runner = new BatchRunner<>();
        runner.execute(deployJobParam.getModuleList(), 3, (threadIndex, dataIndex, module) -> {
            if (module != null) {
                //防止后续多个作业用的同一个作业
                DeployJobVo deployJob = JSON.toJavaObject(jsonObj, DeployJobVo.class);
                deployJob.setSource(JobSource.DEPLOY.getValue());
                deployJob.setInvokeId(invokeId);
                deployJob.setRouteId(invokeId.toString());
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
        AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(credentialUserUuid);
        HttpServletRequest request = UserContext.get().getRequest();
        HttpServletResponse response = UserContext.get().getResponse();
        UserContext.init(credentialUser, authenticationInfo, "+8:00", request, response);
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
                if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
                    throw new ApiRuntimeException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new ApiRuntimeException(error);
                }
            }
        }
    }
}
