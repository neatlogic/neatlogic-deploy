/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.CombopOperationType;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.deploy.exception.DeployAppEnvAuthException;
import neatlogic.framework.deploy.exception.DeployAppScenarioAuthException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.enums.JobLoadTriggerType;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.deploy.dao.mapper.*;
import neatlogic.module.deploy.schedule.plugin.DeployJobAutoFireJob;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeployJobServiceImpl implements DeployJobService {
    private final static Logger logger = LoggerFactory.getLogger(DeployJobServiceImpl.class);
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private DeployVersionMapper deployVersionMapper;

    @Resource
    private DeployResourceMapper deployResourceMapper;

    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Resource
    private DeployBatchJobService deployBatchJobService;

    @Override
    public List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo) {
        List<DeployJobVo> returnList = new ArrayList<>();
        if (Boolean.TRUE.equals(AuthActionChecker.check(DEPLOY_MODIFY.class))) {
            deployJobVo.setIsHasAllAuthority(1);
        } else {
            deployJobVo.setIsHasAllAuthority(0);
            List<String> authorityActionList = new ArrayList<>();
            authorityActionList.add(DeployAppConfigAction.VIEW.getValue());
            deployJobVo.setAuthorityActionList(authorityActionList);
        }
        if (CollectionUtils.isEmpty(deployJobVo.getIdList())) {
            int rowNum = deployJobMapper.searchDeployJobCount(deployJobVo);
            deployJobVo.setRowNum(rowNum);
            List<Long> idList = deployJobMapper.searchDeployJobId(deployJobVo);
            deployJobVo.setIdList(idList);
        }
        if (CollectionUtils.isNotEmpty(deployJobVo.getIdList())) {
            returnList = deployJobMapper.searchDeployJob(deployJobVo);
        }

        //补充子作业信息
        /*经产品核实：含有keyword查询时，匹配到的批量作业需要一次性返回子作业信息*/
        if (CollectionUtils.isNotEmpty(returnList)) {
            List<DeployJobVo> parentJobList = returnList.stream().filter(e -> StringUtils.equals(JobSource.BATCHDEPLOY.getValue(), e.getSource())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(parentJobList)) {
                deployJobVo.setParentIdList(parentJobList.stream().map(AutoexecJobVo::getId).collect(Collectors.toList()));
                deployJobVo.setIdList(null);
                List<AutoexecJobVo> parentInfoJobList = deployJobMapper.getDeploySubJobListByFilter(deployJobVo);
                if (CollectionUtils.isNotEmpty(parentInfoJobList)) {
                    Map<Long, List<AutoexecJobVo>> parentJobChildrenListMap = parentInfoJobList.stream().collect(Collectors.toMap(AutoexecJobVo::getId, AutoexecJobVo::getChildren));
                    for (DeployJobVo jobVo : returnList) {
                        if (StringUtils.equals(jobVo.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                            jobVo.setChildren(parentJobChildrenListMap.get(jobVo.getId()));
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(returnList)) {
            List<AutoexecJobInvokeVo> jobInvokeListWithoutRouteId = new ArrayList<>();
            Map<Long, AutoexecJobRouteVo> jobIdInvokeWithoutRouteIdMap = new HashMap<>();
            Map<String, Set<String>> sourceKeyInvokeIdSetMap = new HashMap<>();
            Map<Long, String> jobIdToRouteIdMap = new HashMap<>();
            List<Long> jobIdList = returnList.stream().map(DeployJobVo::getId).collect(Collectors.toList());
            List<AutoexecJobInvokeVo> jobInvokeList = autoexecJobMapper.getJobInvokeListByJobIdList(jobIdList);
            for (AutoexecJobInvokeVo jobInvokeVo : jobInvokeList) {
                if (jobInvokeVo.getRouteId() != null) {
                    sourceKeyInvokeIdSetMap.computeIfAbsent(jobInvokeVo.getSource(), key -> new HashSet<>()).add(jobInvokeVo.getRouteId());
                } else {
                    jobInvokeListWithoutRouteId.add(jobInvokeVo);
                }
                jobIdToRouteIdMap.put(jobInvokeVo.getJobId(), jobInvokeVo.getRouteId());
            }
            Map<String, AutoexecJobRouteVo> routeMap = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : sourceKeyInvokeIdSetMap.entrySet()) {
                IAutoexecJobSource sourceHandler = AutoexecJobSourceFactory.getHandler(entry.getKey());
                if (sourceHandler == null) {
                    continue;
                }
                List<AutoexecJobRouteVo> list = sourceHandler.getListByUniqueKeyList(new ArrayList<>(entry.getValue()));
                if (CollectionUtils.isNotEmpty(list)) {
                    for (AutoexecJobRouteVo jobRouteVo : list) {
                        routeMap.put(jobRouteVo.getId().toString(), jobRouteVo);
                    }
                }
            }
            //处理routeId为空的情况
            if (CollectionUtils.isNotEmpty(jobInvokeListWithoutRouteId)) {
                for (AutoexecJobInvokeVo autoexecJobInvokeVo : jobInvokeListWithoutRouteId) {
                    IAutoexecJobSource sourceHandler = AutoexecJobSourceFactory.getHandler(autoexecJobInvokeVo.getSource());
                    if (sourceHandler == null) {
                        continue;
                    }
                    List<AutoexecJobRouteVo> list = sourceHandler.getListByUniqueKeyList(null);
                    if (CollectionUtils.isNotEmpty(list)) {
                        jobIdInvokeWithoutRouteIdMap.put(autoexecJobInvokeVo.getJobId(), list.get(0));
                    }
                }
            }
            for (DeployJobVo vo : returnList) {
                String routeId = jobIdToRouteIdMap.get(vo.getId());
                if (routeId != null) {
                    vo.setRouteId(routeId);
                    AutoexecJobRouteVo routeVo = routeMap.get(routeId);
                    if (routeVo != null) {
                        vo.setRoute(routeVo);
                    }
                } else {
                    vo.setRoute(jobIdInvokeWithoutRouteIdMap.get(vo.getId()));
                }
            }
        }
        return returnList;
    }

    /**
     * 校验&&补充作业参数
     *
     * @param deployJobParam 入参
     */
    private void initDeployJobParam(DeployJobVo deployJobParam) {
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystem;
        if (StringUtils.isNotBlank(deployJobParam.getAppSystemAbbrName())) {
            appSystem = iAppSystemMapper.getAppSystemByAbbrName(deployJobParam.getAppSystemAbbrName());
            if (appSystem == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemAbbrName());
            }
            deployJobParam.setAppSystemId(appSystem.getId());
        } else if (deployJobParam.getAppSystemId() != null) {
            appSystem = iAppSystemMapper.getAppSystemById(deployJobParam.getAppSystemId());
            if (appSystem == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppSystemId());
            }
        } else {
            throw new ParamIrregularException("appSystemId | appSystemName");
        }
        deployJobParam.setAppSystemName(appSystem.getName());
        deployJobParam.setAppSystemAbbrName(appSystem.getAbbrName());
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (StringUtils.isNotBlank(deployJobParam.getEnvName())) {
            ResourceVo env = iResourceCrossoverMapper.getAppEnvByName(deployJobParam.getEnvName());
            if (env == null) {
                throw new AppEnvNotFoundException(deployJobParam.getEnvName());
            }
            deployJobParam.setEnvId(env.getId());
        } else if (deployJobParam.getEnvId() != null) {
            ResourceVo env = iResourceCrossoverMapper.getAppEnvById(deployJobParam.getEnvId());
            if (env == null) {
                throw new AppEnvNotFoundException(deployJobParam.getEnvId());
            }
            deployJobParam.setEnvName(env.getName());
        } else {
            throw new ParamIrregularException("envId | envName");
        }

        IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
        if (StringUtils.isNotBlank(deployJobParam.getScenarioName())) {
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioByName(deployJobParam.getScenarioName());
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(deployJobParam.getScenarioName());
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

        deployJobParam.setOperationType(CombopOperationType.PIPELINE.getValue());
        deployJobParam.setIsJobInitParam(true);
    }

    @Override
    public void convertModule(DeployJobVo deployJobParam) {
        convertModule(deployJobParam, new DeployJobModuleVo());
    }

    @Override
    public void convertModule(DeployJobVo deployJobParam, DeployJobModuleVo moduleVo) {
        if (!deployJobParam.getIsJobInitParam()) {
            initDeployJobParam(deployJobParam);
        }
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppModuleVo appModuleVo;
        if (StringUtils.isNotBlank(moduleVo.getAbbrName())) {
            appModuleVo = iAppSystemMapper.getAppModuleByAbbrName(moduleVo.getAbbrName());
            if (appModuleVo == null) {
                throw new CiEntityNotFoundException(moduleVo.getAbbrName());
            }
        } else if (moduleVo.getId() != null) {
            appModuleVo = iAppSystemMapper.getAppModuleById(moduleVo.getId());
            if (appModuleVo == null) {
                throw new CiEntityNotFoundException(moduleVo.getId());
            }
        } else if (deployJobParam.getAppModuleId() != null) {
            appModuleVo = iAppSystemMapper.getAppModuleById(deployJobParam.getAppModuleId());
            if (appModuleVo == null) {
                throw new CiEntityNotFoundException(deployJobParam.getAppModuleId());
            }
        } else {
            throw new ParamIrregularException("module id|name");
        }
        deployJobParam.setAppModuleId(appModuleVo.getId());
        deployJobParam.setAppModuleName(appModuleVo.getName());
        deployJobParam.setAppModuleAbbrName(appModuleVo.getAbbrName());
        deployJobParam.setBuildNo(moduleVo.getBuildNo());
        DeployVersionVo versionVo = null;
        if (StringUtils.isNotBlank(moduleVo.getVersion())) {
            versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), moduleVo.getVersion());
            if (versionVo == null) {
                throw new DeployVersionNotFoundException(deployJobParam.getAppSystemName(), deployJobParam.getAppModuleName(), moduleVo.getVersion());
            }

        }
        if (StringUtils.isNotBlank(deployJobParam.getVersion())) {
            versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getVersion());
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
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        if (CollectionUtils.isNotEmpty(moduleVo.getSelectNodeList())) {
            //如果不存在resourceId则需要补充 resourceId
            for (AutoexecNodeVo autoexecNodeVo : moduleVo.getSelectNodeList()) {
                if (autoexecNodeVo.getId() == null) {
                    ResourceVo resourceVo = resourceCenterResourceCrossoverService.getResourceByIpAndPort(autoexecNodeVo.getIp(), autoexecNodeVo.getPort());
                    if (resourceVo != null) {
                        autoexecNodeVo.setId(resourceVo.getId());
                        autoexecNodeVo.setName(resourceVo.getName());
                    }
                }
            }
        } else {
            //如果selectNodeList 是empty，则发布全部实例
            ResourceVo resourceVo = new ResourceVo(deployJobParam.getAppSystemId(), deployJobParam.getAppModuleId(), deployJobParam.getEnvId());
            int count = deployResourceMapper.getAppInstanceResourceIdCountByAppSystemIdAndModuleIdAndEnvId(resourceVo);
            if (count > 0) {
                int pageCount = PageUtil.getPageCount(count, resourceVo.getPageSize());
                for (int i = 1; i <= pageCount; i++) {
                    resourceVo.setCurrentPage(i);
                    List<Long> instanceIdList = deployResourceMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(resourceVo);
                    if (CollectionUtils.isNotEmpty(instanceIdList)) {
                        List<ResourceVo> instanceList = deployResourceMapper.getAppInstanceResourceListByIdList(instanceIdList);
                        for (ResourceVo instance : instanceList) {
                            AutoexecNodeVo autoexecNodeVo = new AutoexecNodeVo(instance);
                            moduleVo.getSelectNodeList().add(autoexecNodeVo);
                        }
                    }
                }
            }
        }
        executeNodeConfig.setSelectNodeList(moduleVo.getSelectNodeList());
        if (deployJobParam.getExecuteConfig() == null) {
            deployJobParam.setExecuteConfig(new AutoexecCombopExecuteConfigVo());
        }
        deployJobParam.getExecuteConfig().setExecuteNodeConfig(executeNodeConfig);
    }

    @Override
    public JSONObject createJob(DeployJobVo deployJobParam, DeployJobModuleVo module) {
        convertModule(deployJobParam, module);
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(deployJobParam);
        resultJson.put("jobId", deployJobParam.getId());
        resultJson.put("appSystemName", deployJobParam.getAppSystemName());
        resultJson.put("appModuleName", deployJobParam.getAppModuleName());
        resultJson.put("envName", deployJobParam.getEnvName());
        resultJson.put("version", deployJobParam.getVersion());
        resultJson.put("deployJobParam", deployJobParam);
        logger.debug("deployJobCreateParam:" + resultJson.toJSONString());
        return resultJson;
    }

    @Override
    public JSONObject createJob(DeployJobVo deployJobParam) {
        return createJob(deployJobParam, new DeployJobModuleVo());
    }

    @Override
    public JSONObject createJobAndFire(DeployJobVo deployJobParam, DeployJobModuleVo module) throws Exception {
        JSONObject resultJson = createJob(deployJobParam, module);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        deployJobParam.setAction(JobAction.FIRE.getValue());
        fireAction.doService(deployJobParam);
        return resultJson;
    }

    @Override
    public JSONObject createJobAndFire(DeployJobVo deployJobParam) throws Exception {
        return createJobAndFire(deployJobParam, new DeployJobModuleVo());
    }

    @Override
    public JSONObject getJobStatus(Long jobId) {
        JSONObject result = new JSONObject();
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo != null) {
            result.put("jobId", jobVo.getId());
            result.put("jobName", jobVo.getName());
            result.put("status", jobVo.getStatus());
            result.put("statusName", neatlogic.framework.autoexec.constvalue.JobStatus.getText(jobVo.getStatus()));
        }
        return result;
    }

    @Override
    public JSONObject createJobAndSchedule(DeployJobVo deployJobVo, DeployJobModuleVo module) {
        convertModule(deployJobVo, module);
        JSONObject resultJson = new JSONObject();
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(deployJobVo);
        // 保存之后，如果设置的人工触发，那只有点执行按钮才能触发；如果是自动触发，则启动一个定时作业；如果没到点就人工触发了，则取消定时作业，立即执行
        if (JobTriggerType.AUTO.getValue().equals(deployJobVo.getTriggerType())) {
            if (deployJobVo.getPlanStartTime() == null) {
                throw new ParamIrregularException("planStartTime");
            }
            IJob jobHandler = SchedulerManager.getHandler(DeployJobAutoFireJob.class.getName());
            if (jobHandler == null) {
                throw new ScheduleHandlerNotFoundException(DeployJobAutoFireJob.class.getName());
            }
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(deployJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
            jobHandler.reloadJob(jobObjectBuilder.build(), JobLoadTriggerType.INITIAL_CREATE);
        }
        resultJson.put("jobId", deployJobVo.getId());
        resultJson.put("appSystemName", deployJobVo.getAppSystemName());
        resultJson.put("appModuleName", deployJobVo.getAppModuleName());
        resultJson.put("envName", deployJobVo.getEnvName());
        resultJson.put("version", deployJobVo.getVersion());
        resultJson.put("deployJobParam", deployJobVo);
        logger.debug("deployJobScheduleCreateParam:" + resultJson.toJSONString());
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
    public void scheduleAuthCheck(DeployScheduleVo scheduleVo) {
        String type = scheduleVo.getType();
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        IAutoexecScenarioCrossoverMapper iAutoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            Long appSystemId = scheduleVo.getAppSystemId();
            AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId());
            if (appSystemVo == null) {
                throw new AppSystemNotFoundException(scheduleVo.getAppSystemId());
            }
            AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(scheduleVo.getAppModuleId());
            if (appModuleVo == null) {
                throw new AppModuleNotFoundException(scheduleVo.getAppModuleId());
            }
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            ResourceVo env = iResourceCrossoverMapper.getAppEnvById(config.getEnvId());
            if (env == null) {
                throw new AppEnvNotFoundException(config.getEnvId());
            }
            AutoexecScenarioVo scenarioVo = iAutoexecScenarioCrossoverMapper.getScenarioById(config.getScenarioId());
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(config.getScenarioId());
            }
            Set<String> actionSet = DeployAppAuthChecker.builder(appSystemId)
                    .addEnvAction(config.getEnvId())
                    .addScenarioAction(config.getScenarioId())
                    .addOperationAction(DeployAppConfigAction.EDIT.getValue())
                    .addOperationAction(DeployAppConfigAction.EXECUTE.getValue())
                    .check();
            if (!actionSet.contains(config.getEnvId().toString())) {
                throw new DeployAppEnvAuthException(appSystemVo, env);
            }

            if (!actionSet.contains(config.getScenarioId().toString())) {
                throw new DeployAppScenarioAuthException(appSystemVo, scenarioVo);
            }
        } else if (type.equals(ScheduleType.PIPELINE.getValue())) {
            deployBatchJobService.isHasPipelineAuth(scheduleVo.getAppSystemId(), scheduleVo.getPipelineId());
        }
    }
}
