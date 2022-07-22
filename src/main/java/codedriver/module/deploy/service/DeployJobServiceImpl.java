/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.CombopOperationType;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.schedule.plugin.DeployJobAutoFireJob;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeployJobServiceImpl implements DeployJobService {
    private final static Logger logger = LoggerFactory.getLogger(DeployJobServiceImpl.class);
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public void initDeployParam(JSONObject jsonObj) {
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long envId = jsonObj.getLong("envId");
        Long scenarioId = jsonObj.getLong("scenarioId");
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        if (jsonObj.containsKey("appSystemName")) {
            AppSystemVo appSystem = iAppSystemMapper.getAppSystemByAbbrName(jsonObj.getString("appSystemName"), TenantContext.get().getDataDbName());
            if (appSystem == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("appSystemName"));
            }
            appSystemId = appSystem.getId();
        } else if (appSystemId != null) {
            CiEntityVo appSystem = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId) == null) {
                throw new CiEntityNotFoundException(jsonObj.getLong("appSystemId"));
            }
            jsonObj.put("appSystemName", appSystem.getName());
        } else {
            throw new ParamIrregularException("appSystemId | appSystemName");
        }

        if (jsonObj.containsKey("envName")) {
            envId = iCiEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APPEnv", jsonObj.getString("envName"));
            if (envId == null) {
                throw new CiEntityNotFoundException(jsonObj.getString("envName"));
            }
        } else if (envId != null) {
            CiEntityVo envEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
            if (envEntity == null) {
                throw new CiEntityNotFoundException(jsonObj.getLong("envId"));
            }
            jsonObj.put("envName", envEntity.getName());
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
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioById(scenarioId);
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(jsonObj.getLong("scenarioId"));
            }
            jsonObj.put("scenarioName", scenarioVo.getName());
        } else {
            throw new ParamIrregularException("scenarioId");
        }

        if (!jsonObj.containsKey("source")) {
            jsonObj.put("source", JobSource.DEPLOY.getValue());
        }
        jsonObj.put("operationType", CombopOperationType.PIPELINE.getValue());
    }

    @Override
    public void convertModule(JSONObject jsonObj, JSONObject moduleJson) {
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        Long appModuleId = moduleJson.getLong("id");
        if (moduleJson.containsKey("name")) {
            AppModuleVo appModuleVo = iAppSystemMapper.getAppModuleByAbbrName(moduleJson.getString("name"), TenantContext.get().getDataDbName());
            if (appModuleVo == null) {
                throw new CiEntityNotFoundException(moduleJson.getString("name"));
            }
            jsonObj.put("appModuleId", appModuleVo.getId());
            jsonObj.put("appModuleName", moduleJson.getString("name"));
        } else if (appModuleId != null) {
            CiEntityVo entityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId);
            if (entityVo == null) {
                throw new CiEntityNotFoundException(moduleJson.getLong("id"));
            }
            jsonObj.put("appModuleId", moduleJson.getLong("id"));
            jsonObj.put("appModuleName", entityVo.getName());
        } else {
            throw new AppModuleNotFoundException();
        }
        jsonObj.put("buildNo", moduleJson.getInteger("buildNo"));
        jsonObj.put("version", moduleJson.getString("version"));
        JSONObject executeConfig = jsonObj.getJSONObject("executeConfig");
        executeConfig.put("executeNodeConfig", new JSONObject() {{
            JSONArray selectNodeArray = moduleJson.getJSONArray("selectNodeList");
            if(CollectionUtils.isNotEmpty(selectNodeArray)){
                put("selectNodeList", selectNodeArray);
            }else{//如果selectNodeList 是empty，则发布全部实例
               put("filter",new JSONObject(){{
                   put("appSystemIdList", Collections.singletonList(jsonObj.getLong("appSystemId")));
                   put("appModuleIdList", Collections.singletonList(jsonObj.getLong("appModuleId")));
               }});
            }
        }});
        jsonObj.put("name", jsonObj.getString("appSystemName") + "/" + jsonObj.getString("appModuleName") + "/" + jsonObj.getString("envName") + "/" + jsonObj.getString("scenarioName"));
    }

    @Override
    public JSONObject createJob(JSONObject jsonObj) {
        JSONObject resultJson = new JSONObject();
        try {
            IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
            AutoexecJobVo jobVo = autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jsonObj, false);
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            jobVo.setAction(JobAction.FIRE.getValue());
            jobVo.setIsFirstFire(1);
            fireAction.doService(jobVo);
            resultJson.put("jobId", jobVo.getId());
            resultJson.put("appModuleName", jsonObj.getString("appModuleName"));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            resultJson.put("appSystemName", jsonObj.getString("appSystemName"));
            resultJson.put("appModuleName", jsonObj.getString("appModuleName"));
            resultJson.put("errorMsg", ex.getMessage());
        }
        return resultJson;
    }

    @Override
    public JSONObject createScheduleJob(JSONObject jsonObj) {
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        AutoexecJobVo jobVo = autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jsonObj, false);
        try {
            // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
            if (JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType())) {
                IJob jobHandler = SchedulerManager.getHandler(DeployJobAutoFireJob.class.getName());
                if (jobHandler == null) {
                    throw new ScheduleHandlerNotFoundException(DeployJobAutoFireJob.class.getName());
                }
                JobObject.Builder jobObjectBuilder = new JobObject.Builder(jobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
                jobHandler.reloadJob(jobObjectBuilder.build());
            }
            resultJson.put("jobId", jobVo.getId());
            resultJson.put("appModuleName", jsonObj.getString("appModuleName"));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            resultJson.put("errorMsg", ex.getMessage());
        }
        return resultJson;
    }


    @Override
    @Deprecated
    public Long getOperationId(JSONObject jsonObj) {
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
}
