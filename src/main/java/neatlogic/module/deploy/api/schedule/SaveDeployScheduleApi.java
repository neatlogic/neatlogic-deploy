/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.api.schedule;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.dto.version.DeploySystemModuleVersionVo;
import neatlogic.framework.deploy.exception.DeployPipelineNotFoundException;
import neatlogic.framework.deploy.exception.DeployScheduleNameRepeatException;
import neatlogic.framework.deploy.exception.DeployScheduleNotFoundException;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.framework.scheduler.exception.ScheduleIllegalParameterException;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.schedule.plugin.DeployJobScheduleJob;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private DeployPipelineMapper deployPipelineMapper;
    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getToken() {
        return "deploy/schedule/save";
    }

    @Override
    public String getName() {
        return "保存定时作业信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "定时作业id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "定时作业名称"),
            @Param(name = "beginTime", type = ApiParamType.LONG, desc = "开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "结束时间"),
            @Param(name = "cron", type = ApiParamType.STRING, isRequired = true, desc = "corn表达式"),
            @Param(name = "isActive", type = ApiParamType.ENUM, isRequired = true, rule = "0,1", desc = "是否激活(0:禁用，1：激活)"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行配置信息"),
            @Param(name = "type", type = ApiParamType.ENUM, member = ScheduleType.class, isRequired = true, desc = "作业类型"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "pipelineId", type = ApiParamType.LONG, desc = "流水线id"),
            @Param(name = "pipelineType", type = ApiParamType.ENUM, member = PipelineType.class, desc = "流水线类型")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.STRING, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "保存定时作业信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IJob jobHandler = SchedulerManager.getHandler(DeployJobScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(DeployJobScheduleJob.class.getName());
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        String userUuid = UserContext.get().getUserUuid(true);
        DeployScheduleVo scheduleVo = paramObj.toJavaObject(DeployScheduleVo.class);
        String cron = scheduleVo.getCron();
        if (!CronExpression.isValidExpression(cron)) {
            throw new ScheduleIllegalParameterException(cron);
        }
        if (deployScheduleMapper.checkScheduleNameIsExists(scheduleVo) > 0) {
            throw new DeployScheduleNameRepeatException(scheduleVo.getName());
        }
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId());
            if (appSystemVo == null) {
                throw new AppSystemNotFoundException(scheduleVo.getAppSystemId());
            }
            AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(scheduleVo.getAppModuleId());
            if (appModuleVo == null) {
                throw new AppModuleNotFoundException(scheduleVo.getAppModuleId());
            }
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            Long scenarioId = config.getScenarioId();
            if (scenarioId == null) {
                throw new ParamNotExistsException("场景ID（config.scenarioId）");
            }
            IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
            AutoexecScenarioVo autoexecScenarioVo = autoexecScenarioCrossoverMapper.getScenarioById(scenarioId);
            if (autoexecScenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(scenarioId);
            }
            Long envId = config.getEnvId();
            if (envId == null) {
                throw new ParamNotExistsException("环境ID（config.envId）");
            }
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceVo resourceVo = resourceCrossoverMapper.getAppEnvById(envId);
            if (resourceVo == null) {
                throw new AppEnvNotFoundException(envId);
            }
            Integer roundCount = config.getRoundCount();
            if (roundCount == null) {
                throw new ParamNotExistsException("分配数量（config.roundCount）");
            }
            List<DeployJobModuleVo> moduleList = config.getModuleList();
            if (CollectionUtils.isEmpty(moduleList)) {
                throw new ParamNotExistsException("模块版本列表（config.moduleList）");
            }
            Map<Long, AppModuleVo> appModuleMap = new HashMap<>();
            List<Long> appModuleIdList = moduleList.stream().map(DeployJobModuleVo::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                List<AppModuleVo> appModuleList = appSystemMapper.getAppModuleListByIdList(appModuleIdList);
                appModuleMap = appModuleList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
            }
            for (DeployJobModuleVo deployJobModuleVo : moduleList) {
                Long appModuleId = deployJobModuleVo.getId();
                if (appModuleId == null) {
                    throw new ParamNotExistsException("模块ID（config.moduleList.id）");
                }
                AppModuleVo appModule = appModuleMap.get(appModuleId);
                if (appModule == null) {
                    throw new AppModuleNotFoundException(appModuleId);
                }
            }
        } else if (type.equals(ScheduleType.PIPELINE.getValue())) {
            PipelineVo pipelineVo = deployPipelineMapper.getPipelineSimpleInfoById(scheduleVo.getPipelineId());
            if (pipelineVo == null) {
                throw new DeployPipelineNotFoundException(scheduleVo.getPipelineId());
            }
            String pipelineType = scheduleVo.getPipelineType();
            if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                scheduleVo.setAppSystemId(pipelineVo.getAppSystemId());
            }
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            List<DeploySystemModuleVersionVo> deploySystemModuleVersionList = config.getAppSystemModuleVersionList();
            if (CollectionUtils.isEmpty(deploySystemModuleVersionList)) {
                throw new ParamNotExistsException("应用模块环境（场景）版本列表（config.deploySystemModuleVersionList）");
            }
            Map<Long, AppSystemVo> appSystemMap = new HashMap<>();
                    List<Long> appSystemIdList = deploySystemModuleVersionList.stream().map(DeploySystemModuleVersionVo::getAppSystemId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                List<AppSystemVo> appSystemList = appSystemMapper.getAppSystemListByIdList(appSystemIdList);
                appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
            }
            Map<Long, AppModuleVo> appModuleMap = new HashMap<>();
                    List<Long> appModuleIdList = deploySystemModuleVersionList.stream().map(DeploySystemModuleVersionVo::getAppModuleId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                List<AppModuleVo> appModuleList = appSystemMapper.getAppModuleListByIdList(appModuleIdList);
                appModuleMap = appModuleList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
            }
            for (DeploySystemModuleVersionVo deploySystemModuleVersionVo : deploySystemModuleVersionList) {
                Long appSystemId = deploySystemModuleVersionVo.getAppSystemId();
                if (appSystemId == null) {
                    throw new ParamNotExistsException("模块ID（config.deploySystemModuleVersionList.appSystemId）");
                }
                AppSystemVo appSystem = appSystemMap.get(appSystemId);
                if (appSystem == null) {
                    throw new AppSystemNotFoundException(appSystemId);
                }
                Long appModuleId = deploySystemModuleVersionVo.getAppModuleId();
                if (appModuleId == null) {
                    throw new ParamNotExistsException("应用ID（config.deploySystemModuleVersionList.appModuleId）");
                }
                AppModuleVo appModule = appModuleMap.get(appModuleId);
                if (appModule == null) {
                    throw new AppModuleNotFoundException(appModuleId);
                }
                Long versionId = deploySystemModuleVersionVo.getVersionId();
                if (versionId == null) {
                    throw new ParamNotExistsException("模块版本（config.deploySystemModuleVersionList.versionId）");
                }
            }
        }

        JobObject jobObject = new JobObject.Builder(scheduleVo.getUuid(), jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid)
                .withCron(scheduleVo.getCron()).withBeginTime(scheduleVo.getBeginTime())
                .withEndTime(scheduleVo.getEndTime())
                .setType("private")
                .build();
        Long id = paramObj.getLong("id");
        if (id != null) {
            DeployScheduleVo oldScheduleVo = deployScheduleMapper.getScheduleById(id);
            if (oldScheduleVo == null) {
                throw new DeployScheduleNotFoundException(id);
            }
            scheduleVo.setLcu(userUuid);
            scheduleVo.setUuid(oldScheduleVo.getUuid());
            deployScheduleMapper.updateSchedule(scheduleVo);
            schedulerManager.unloadJob(jobObject);
        } else {
            scheduleVo.setFcu(userUuid);
            deployScheduleMapper.insertSchedule(scheduleVo);
        }

        if (scheduleVo.getIsActive().intValue() == 1) {
            schedulerManager.loadJob(jobObject);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("id", scheduleVo.getId());
        return resultObj;
    }

    public IValid name() {
        return value -> {
            DeployScheduleVo vo = JSONObject.toJavaObject(value, DeployScheduleVo.class);
            if (deployScheduleMapper.checkScheduleNameIsExists(vo) > 0) {
                return new FieldValidResultVo(new DeployScheduleNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
