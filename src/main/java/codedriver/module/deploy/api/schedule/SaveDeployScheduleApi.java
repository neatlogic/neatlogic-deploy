/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.job.DeployJobModuleVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.deploy.dto.version.DeploySystemModuleVersionVo;
import codedriver.framework.deploy.exception.DeployPipelineNotFoundException;
import codedriver.framework.deploy.exception.DeployScheduleNameRepeatException;
import codedriver.framework.deploy.exception.DeployScheduleNotFoundException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.framework.scheduler.exception.ScheduleIllegalParameterException;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.schedule.plugin.DeployJobScheduleJob;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
        String schemaName = TenantContext.get().getDataDbName();
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
            AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId(), schemaName);
            if (appSystemVo == null) {
                throw new AppSystemNotFoundException(scheduleVo.getAppSystemId());
            }
            AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(scheduleVo.getAppModuleId(), schemaName);
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
            ResourceVo resourceVo = resourceCrossoverMapper.getAppEnvById(envId, schemaName);
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
            String pipelineType = scheduleVo.getPipelineType();
            if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                // TODO 应用流水线功能还没实现
            } else if (pipelineType.equals(PipelineType.GLOBAL.getValue())) {
                String name = deployPipelineMapper.getPipelineNameById(scheduleVo.getPipelineId());
                if (StringUtils.isBlank(name)) {
                    throw new DeployPipelineNotFoundException(scheduleVo.getPipelineId());
                }
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
