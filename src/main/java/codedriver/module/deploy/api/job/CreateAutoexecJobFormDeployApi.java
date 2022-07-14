/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.CombopOperationType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamIrregularException;
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
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appSystemName", type = ApiParamType.STRING, desc = "应用系统名，如果入参也有appSystemId，则会以appSystemName为准"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "appModuleName", type = ApiParamType.STRING, desc = "模块名，如果入参也有appModuleId，则会以appModuleName为准"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境id，如果入参也有envId，则会以envName为准"),
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
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId");
        Long envId = jsonObj.getLong("envId");
        Long scenarioId = jsonObj.getLong("scenarioId");
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (jsonObj.containsKey("appSystemName")) {
            appSystemId = iCiEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APP", jsonObj.getString("appSystemName"));
            if (appSystemId == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("appSystemName"));
            }
        } else if (appSystemId != null) {
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId) == null) {
                throw new CiEntityNotFoundException(jsonObj.getLong("appSystemId"));
            }
        } else {
            throw new ParamIrregularException("appSystemId | appSystemName");
        }

        if (jsonObj.containsKey("appModuleName")) {
            appModuleId = iCiEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APPComponent", jsonObj.getString("appModuleName"));
            if (appModuleId == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("appModuleName"));
            }
        } else if (appModuleId != null) {
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
                throw new CiEntityNotFoundException(jsonObj.getLong("appModuleId"));
            }
        } else {
            throw new ParamIrregularException("appModuleId | appModuleName");
        }

        if (jsonObj.containsKey("envName")) {
            envId = iCiEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APPEnv", jsonObj.getString("envName"));
            if (envId == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("envName"));
            }
        } else if (envId != null) {
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(envId) == null) {
                throw new CiEntityNotFoundException(jsonObj.getLong("envId"));
            }
        } else {
            throw new ParamIrregularException("envId | envName");
        }

        IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
        if (jsonObj.containsKey("scenarioName")) {
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioByName(jsonObj.getString("scenarioName"));
            if (scenarioVo == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("scenarioName"));
            }
            jsonObj.put("scenarioId", scenarioVo.getId());
        } else if (scenarioId != null) {
            if (autoexecScenarioCrossoverMapper.getScenarioById(scenarioId) == null) {
                throw new AutoexecScenarioIsNotFoundException(jsonObj.getLong("scenarioId"));
            }
        } else {
            throw new ParamIrregularException("scenarioId");
        }

        jsonObj.put("operationType", CombopOperationType.PIPELINE.getValue());
        Long invokeId = getOperationId(jsonObj);
        jsonObj.put("operationId", invokeId);
        jsonObj.put("invokeId", invokeId);
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        AutoexecJobVo jobVo = autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jsonObj, false);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setIsFirstFire(1);
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
