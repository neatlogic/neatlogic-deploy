/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.CombopOperationType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecJobFormDeployApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "作业创建(来自发布)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, desc = "编译号"),
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "版本"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源 itsm|human|deploy   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "threadCount", type = ApiParamType.LONG, isRequired = true, desc = "并发线程,2的n次方 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
    })
    @Output({
    })
    @Description(desc = "作业创建（来自发布）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("operationType", CombopOperationType.PIPELINE.getValue());
        Long invokeId = getOperationId(jsonObj);
        jsonObj.put("operationId", invokeId);
        jsonObj.put("invokeId", invokeId);
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        AutoexecJobVo jobVo = autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jsonObj, false);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setIsFirstFire(1);
        JSONObject environment = new JSONObject();
        //environment.put("_VERSION",jsonObj.getString("version"));
        //environment.put("_BUILD_NO",jsonObj.getInteger("buildNo"));
        jobVo.setEnvironment(environment);
        fireAction.doService(jobVo);
        return new JSONObject() {{
            put("jobId", jobVo.getId());
        }};
    }

    /**
     * 获取来源id
     *
     * @param jsonObj 入参
     * @return 来源id
     */
    private Long getOperationId(JSONObject jsonObj) {
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId");
        Long envId = jsonObj.getLong("envId");
        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        Map<String, Long> operationIdMap = appConfigVoList.stream().collect(Collectors.toMap(o -> o.getAppSystemId().toString() + "-" + o.getAppModuleId().toString() + "-" + o.getEnvId().toString(), DeployAppConfigVo::getId));
        Long operationId = operationIdMap.get(appSystemId.toString() + "-" + appModuleId.toString() + "-" + envId.toString());
        if (operationId == null) {
            operationId = operationIdMap.get(appSystemId + "-" + appModuleId + "-0");
        }
        if (operationId == null) {
            operationId = operationIdMap.get(appSystemId + "-0" + "-0");
        }
        if (operationId == null) {
            throw new DeployAppConfigNotFoundException(appModuleId);
        }
        return operationId;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/deploy/create";
    }
}
