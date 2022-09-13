/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.CombopOperationType;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.job.DeployJobModuleVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.deploy.exception.DeployAppModuleEnvInstanceNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.schedule.plugin.DeployJobAutoFireJob;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeployJobServiceImpl implements DeployJobService {
    private final static Logger logger = LoggerFactory.getLogger(DeployJobServiceImpl.class);
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployVersionMapper deployVersionMapper;

    @Override
    public List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo) {
        if (CollectionUtils.isEmpty(deployJobVo.getIdList())) {
            int rowNum = deployJobMapper.searchDeployJobCount(deployJobVo);
            deployJobVo.setRowNum(rowNum);
            List<Long> idList = deployJobMapper.searchDeployJobId(deployJobVo);
            deployJobVo.setIdList(idList);
        }
        if (CollectionUtils.isNotEmpty(deployJobVo.getIdList())) {
            return deployJobMapper.searchDeployJob(deployJobVo);
        }
        return null;
    }

    @Override
    public void initDeployParam(DeployJobVo deployJobParam, Boolean isBatch) {
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        if (StringUtils.isNotBlank(deployJobParam.getAppSystemName())) {
            AppSystemVo appSystem = iAppSystemMapper.getAppSystemByAbbrName(deployJobParam.getAppSystemAbbrName(), TenantContext.get().getDataDbName());
            if (appSystem == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemName());
            }
            deployJobParam.setAppSystemId(appSystem.getId());
            deployJobParam.setAppSystemAbbrName(appSystem.getAbbrName());
        } else if (deployJobParam.getAppSystemId() != null) {
            AppSystemVo appSystem = iAppSystemMapper.getAppSystemById(deployJobParam.getAppSystemId(), TenantContext.get().getDataDbName());
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobParam.getAppSystemId()) == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemId());
            }
            deployJobParam.setAppSystemName(appSystem.getName());
            deployJobParam.setAppSystemAbbrName(appSystem.getAbbrName());
        } else {
            throw new ParamIrregularException("appSystemId | appSystemName");
        }
        if (StringUtils.isNotBlank(deployJobParam.getEnvName())) {
            Long envId = iCiEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APPEnv", deployJobParam.getEnvName());
            if (envId == null) {
                throw new CiEntityNotFoundException(deployJobParam.getEnvName());
            }
            deployJobParam.setEnvId(envId);
        } else if (deployJobParam.getEnvId() != null) {
            CiEntityVo envEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobParam.getEnvId());
            if (envEntity == null) {
                throw new CiEntityNotFoundException(deployJobParam.getEnvId());
            }
            deployJobParam.setEnvName(envEntity.getName());
        } else {
            throw new ParamIrregularException("envId | envName");
        }

        IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
        if (StringUtils.isNotBlank(deployJobParam.getScenarioName())) {
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioByName(deployJobParam.getScenarioName());
            if (scenarioVo == null) {
                throw new CiEntityNotFoundException(deployJobParam.getScenarioName());
            }
            deployJobParam.setScenarioId(scenarioVo.getId());
        } else if (deployJobParam.getScenarioId() != null) {
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioById(deployJobParam.getScenarioId());
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(deployJobParam.getScenarioId());
            }
            deployJobParam.setScenarioName(scenarioVo.getName());
        } else {
            throw new ParamIrregularException("scenarioId | scenarioName");
        }

        if (StringUtils.isBlank(deployJobParam.getSource())) {
            deployJobParam.setSource(JobSource.DEPLOY.getValue());
        }
        deployJobParam.setOperationType(CombopOperationType.PIPELINE.getValue());
        if (isBatch) {
            convertSingleModule(deployJobParam);
        } else {
            convertModuleList(deployJobParam);
        }
    }

    @Override
    public void convertModuleList(DeployJobVo deployJobParam) {
        for (DeployJobModuleVo moduleVo : deployJobParam.getModuleList()) {
            IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
            AppModuleVo appModuleVo;
            if (StringUtils.isNotBlank(moduleVo.getName())) {
                appModuleVo = iAppSystemMapper.getAppModuleByAbbrName(moduleVo.getAbbrName(), TenantContext.get().getDataDbName());
                if (appModuleVo == null) {
                    throw new CiEntityNotFoundException(moduleVo.getAbbrName());
                }
            } else if (moduleVo.getId() != null) {
                appModuleVo = iAppSystemMapper.getAppModuleById(moduleVo.getId(), TenantContext.get().getDataDbName());
                if (appModuleVo == null) {
                    throw new CiEntityNotFoundException(moduleVo.getId());
                }
            } else {
                throw new ParamIrregularException("moduleList.id|name");
            }
            deployJobParam.setAppModuleId(appModuleVo.getId());
            deployJobParam.setAppModuleName(appModuleVo.getName());
            deployJobParam.setAppModuleAbbrName(appModuleVo.getAbbrName());
            deployJobParam.setBuildNo(moduleVo.getBuildNo());
            DeployVersionVo versionVo = null;
            if (StringUtils.isNotBlank(moduleVo.getVersion())) {
                versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), moduleVo.getVersion());
                if (versionVo == null) {
                    throw new DeployVersionNotFoundException(deployJobParam.getAppSystemName(), deployJobParam.getAppModuleName(), deployJobParam.getVersion());
                }

            } else if (deployJobParam.getVersionId() != null) {
                versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersionId(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getVersionId());
                if (versionVo == null) {
                    throw new DeployVersionNotFoundException(deployJobParam.getAppSystemName(), deployJobParam.getAppModuleName(), deployJobParam.getVersion());
                }
            }
            if (versionVo != null) {
                deployJobParam.setVersionId(versionVo.getId());
                deployJobParam.setVersion(versionVo.getVersion());
            }
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
            if (CollectionUtils.isNotEmpty(moduleVo.getSelectNodeList())) {
                //如果不存在resourceId则需要补充 resourceId
                for (AutoexecNodeVo autoexecNodeVo : moduleVo.getSelectNodeList()) {
                    if (autoexecNodeVo.getId() == null) {
                        ResourceVo resourceVo = resourceCrossoverMapper.getResourceByIpAndPort(TenantContext.get().getDataDbName(), autoexecNodeVo.getIp(), autoexecNodeVo.getPort());
                        if (resourceVo != null) {
                            autoexecNodeVo.setId(resourceVo.getId());
                            autoexecNodeVo.setName(resourceVo.getName());
                        }
                    }
                }
            } else {
                //如果selectNodeList 是empty，则发布全部实例
                List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getEnvId()), TenantContext.get().getDataDbName());
                if (CollectionUtils.isNotEmpty(instanceIdList)) {
                    List<ResourceVo> instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdList(instanceIdList, TenantContext.get().getDataDbName());
                    for (ResourceVo instance : instanceList) {
                        AutoexecNodeVo autoexecNodeVo = new AutoexecNodeVo(instance);
                        moduleVo.getSelectNodeList().add(autoexecNodeVo);
                    }
                }
            }
            executeNodeConfig.setSelectNodeList(moduleVo.getSelectNodeList());
            if (deployJobParam.getExecuteConfig() == null) {
                deployJobParam.setExecuteConfig(new AutoexecCombopExecuteConfigVo());
            }
            deployJobParam.getExecuteConfig().setExecuteNodeConfig(executeNodeConfig);

        }
    }

    @Override
    public void convertSingleModule(DeployJobVo deployJobParam) {
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppModuleVo appModuleVo = iAppSystemMapper.getAppModuleById(deployJobParam.getAppModuleId(), TenantContext.get().getDataDbName());
        if (appModuleVo == null) {
            throw new CiEntityNotFoundException(deployJobParam.getAppModuleId());
        }
        deployJobParam.setAppModuleName(appModuleVo.getName());
        deployJobParam.setAppModuleAbbrName(appModuleVo.getAbbrName());
        if (deployJobParam.getVersionId() != null) {
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersionId(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getVersionId());
            if (versionVo == null) {
                throw new DeployVersionNotFoundException(deployJobParam.getAppSystemName(), deployJobParam.getAppModuleName(), deployJobParam.getVersion());
            }
            deployJobParam.setVersion(versionVo.getVersion());
        } else {
            throw new ParamIrregularException("versionId");
        }
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        //发布全部实例
        List<AutoexecNodeVo> selectNodeList = new ArrayList<>();
        List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getEnvId()), TenantContext.get().getDataDbName());
        if (CollectionUtils.isNotEmpty(instanceIdList)) {
            List<ResourceVo> instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdList(instanceIdList, TenantContext.get().getDataDbName());
            for (ResourceVo instance : instanceList) {
                AutoexecNodeVo autoexecNodeVo = new AutoexecNodeVo(instance);
                selectNodeList.add(autoexecNodeVo);
            }
        } else {
            throw new DeployAppModuleEnvInstanceNotFoundException(deployJobParam.getAppSystemName(), deployJobParam.getAppModuleName(), deployJobParam.getEnvName());
        }
        executeNodeConfig.setSelectNodeList(selectNodeList);
        if (deployJobParam.getExecuteConfig() == null) {
            deployJobParam.setExecuteConfig(new AutoexecCombopExecuteConfigVo());
        }
        deployJobParam.getExecuteConfig().setExecuteNodeConfig(executeNodeConfig);

        deployJobParam.setName(deployJobParam.getAppSystemAbbrName() + "/" + deployJobParam.getAppModuleAbbrName() + "/" + deployJobParam.getEnvName() + (StringUtils.isBlank(deployJobParam.getVersion()) ? StringUtils.EMPTY : "/" + deployJobParam.getVersion()));
    }

    @Override
    public JSONObject createBatchJob(DeployJobVo deployJobParam) {
        initDeployParam(deployJobParam, true);
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(deployJobParam);
        resultJson.put("jobId", deployJobParam.getId());
        resultJson.put("appSystemName", deployJobParam.getAppSystemName());
        resultJson.put("appModuleName", deployJobParam.getAppModuleName());
        return resultJson;
    }

    @Override
    public JSONObject createJob(DeployJobVo deployJobParam) throws Exception {
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(deployJobParam);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        deployJobParam.setAction(JobAction.FIRE.getValue());
        deployJobParam.setIsFirstFire(1);
        fireAction.doService(deployJobParam);
        resultJson.put("jobId", deployJobParam.getId());
        resultJson.put("appSystemName", deployJobParam.getAppSystemName());
        resultJson.put("appModuleName", deployJobParam.getAppModuleName());
        return resultJson;
    }

    @Override
    public JSONObject createScheduleJob(DeployJobVo deployJobVo) {
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(deployJobVo);
        // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
        if (JobTriggerType.AUTO.getValue().equals(deployJobVo.getTriggerType())) {
            if (deployJobVo.getStartTime() == null) {
                throw new ParamIrregularException("planStartTime");
            }
            IJob jobHandler = SchedulerManager.getHandler(DeployJobAutoFireJob.class.getName());
            if (jobHandler == null) {
                throw new ScheduleHandlerNotFoundException(DeployJobAutoFireJob.class.getName());
            }
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(deployJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
            jobHandler.reloadJob(jobObjectBuilder.build());
        }
        resultJson.put("jobId", deployJobVo.getId());
        resultJson.put("appModuleName", deployJobVo.getAppModuleName());
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

    @Override
    public JSONArray createDeployJob(DeployJobVo deployJobParam) {
        JSONArray result = new JSONArray();
        initDeployParam(deployJobParam, false);
        BatchRunner<DeployJobModuleVo> runner = new BatchRunner<>();
        runner.execute(deployJobParam.getModuleList(), 3, module -> {
            if (module != null) {
                try {
                    if (StringUtils.isNotBlank(deployJobParam.getTriggerType())) {
                        result.add(createScheduleJob(deployJobParam));
                    } else {
                        result.add(createJob(deployJobParam));
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("appSystemName", deployJobParam.getAppSystemName());
                    resultJson.put("appModuleName", deployJobParam.getAppModuleName());
                    resultJson.put("errorMsg", ex.getMessage());
                    result.add(resultJson);
                }

            }
        }, "DEPLOY-JOB-CREATE");
        return result;
    }
}
