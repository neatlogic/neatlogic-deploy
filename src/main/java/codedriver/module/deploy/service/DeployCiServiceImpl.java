package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.util.StringUtil;
import codedriver.framework.deploy.constvalue.DeployCiRepoType;
import codedriver.framework.deploy.constvalue.DeployCiTriggerType;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.job.DeployJobModuleVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.deploy.dto.version.DeploySystemModuleVersionVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.framework.util.TimeUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DeployCiServiceImpl implements DeployCiService {

    static Logger logger = LoggerFactory.getLogger(DeployCiServiceImpl.class);

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployPipelineMapper deployPipelineMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    DeployJobService deployJobService;

    @Resource
    DeployBatchJobService deployBatchJobService;

    public RunnerVo getRandomRunnerBySystemIdAndModuleId(CiEntityVo system, CiEntityVo module) {
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(system.getId(), module.getId());
        if (runnerGroupVo == null || CollectionUtils.isEmpty(runnerGroupVo.getRunnerList())) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(system.getName() + "(" + system.getId() + ")", module.getName() + "(" + module.getId() + ")");
        }
        List<RunnerVo> runnerList = runnerGroupVo.getRunnerList();
        return runnerList.get((int) (Math.random() * runnerList.size()));
    }

    @Override
    public Long createJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception {
        /*
          只要场景包含build，那么新建buildNo和新建版本（如果版本不存在）
          如果场景只包含deploy，那么新建版本（如果版本不存在）
          如果场景不含build和deploy，那么buildNo和版本都不新建
        */
        String repoTypeName = StringUtil.toFirstCharUpperCase(repoType.getValue());
        Long scenarioId = ci.getConfig().getLong("scenarioId");
        if (scenarioId == null) {
            logger.error("{} callback error. Missing scenarioId in ci config, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployCiScenarioIdLostException();
        }
        Long envId = ci.getConfig().getLong("envId");
        if (envId == null) {
            logger.error("{} callback error. Missing envId in ci config, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployCiEnvIdLostException();
        }
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(ci.getAppSystemId())
                .withAppModuleId(ci.getAppModuleId())
                .withEnvId(envId)
                .isHasBuildOrDeployTypeTool(true)
                .getConfig();
        if (deployPipelineConfigVo == null) {
            logger.error("{} callback error. Deploy app config not found, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployAppConfigNotFoundException(ci.getAppSystemId());
        }
        List<AutoexecCombopScenarioVo> scenarioList = deployPipelineConfigVo.getScenarioList();
        Optional<AutoexecCombopScenarioVo> scenarioOptional = scenarioList.stream().filter(o -> Objects.equals(o.getScenarioId(), scenarioId)).findFirst();
        if (!scenarioOptional.isPresent()) {
            logger.error("{} callback error. Scenario not found, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new AutoexecScenarioIsNotFoundException(scenarioId);
        }
        AutoexecCombopScenarioVo scenarioVo = scenarioOptional.get();
        // 如果版本不存在且包含build或deploy工具，那么新建版本
        if (deployVersion == null && (Objects.equals(scenarioVo.getIsHasBuildTypeTool(), 1) || Objects.equals(scenarioVo.getIsHasDeployTypeTool(), 1))) {
            deployVersion = new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId(), 0);
            deployVersionMapper.insertDeployVersion(deployVersion);
        }
        String triggerType = ci.getTriggerType();
        Date triggerTime = getTriggerTime(ci.getTriggerTime());
        if (DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType())) {
            triggerType = DeployCiTriggerType.AUTO.getValue();
        }
        DeployJobVo deployJobParam = new DeployJobVo(ci.getAppSystemId(), scenarioId, envId, triggerType, triggerTime, ci.getConfig().getInteger("roundCount"), ci.getConfig().getJSONObject("param"));
        JSONArray selectNodeList = ci.getConfig().getJSONArray("selectNodeList");
        DeployJobModuleVo moduleVo = new DeployJobModuleVo(ci.getAppModuleId(), deployVersion != null ? deployVersion.getVersion() : null, CollectionUtils.isNotEmpty(selectNodeList) ? selectNodeList.toJavaList(AutoexecNodeVo.class) : null);
        // 包含编译工具则新建buildNo
        if (Objects.equals(scenarioVo.getIsHasBuildTypeTool(), 1)) {
            moduleVo.setBuildNo(-1);
        }
        deployJobParam.setModuleList(Collections.singletonList(moduleVo));
        if (!Objects.equals(ci.getTriggerType(), DeployCiTriggerType.INSTANT.getValue())) {
            deployJobService.createScheduleJob(deployJobParam, moduleVo);
        } else {
            deployJobService.createJobAndFire(deployJobParam, moduleVo);
        }
        return deployJobParam.getId();
    }

    @Override
    public Long createBatchJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception {
        /*
           1、筛选出超级流水线中属于当前模块的子作业，检查每个子作业的场景是否包含build工具以决定是否要新建版本
           2、用筛选后的流水线创建批量作业
        */
        String repoTypeName = StringUtil.toFirstCharUpperCase(repoType.getValue());
        Long pipelineId = ci.getConfig().getLong("pipelineId");
        if (pipelineId == null) {
            logger.error("{} callback error. Missing pipelineId in ci config, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployCiPipelineIdLostException();
        }
        PipelineVo pipeline = deployPipelineMapper.getPipelineBaseInfoByIdAndModuleId(pipelineId, ci.getAppModuleId());
        if (pipeline == null) {
            logger.error("{} callback error. pipeline not found, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployPipelineNotFoundException(pipelineId);
        }
        // 判断超级流水线中是否含有编译工具的作业模版
        DeployPipelineConfigManager.judgeHasBuildOrDeployTypeToolInPipeline(ci.getAppSystemId(), ci.getAppModuleId(), pipeline);
        if (deployVersion == null && (pipeline.getIsHasBuildTypeTool() == 1 || pipeline.getIsHasDeployTypeTool() == 1)) {
            deployVersion = new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId(), 0);
            deployVersionMapper.insertDeployVersion(deployVersion);
        }
        String jobName = ci.getConfig().getString("jobName");
        if (StringUtils.isBlank(jobName)) {
            logger.error("{} callback error. Missing jobName in ci config, ciId: {}, callback params: {}", repoTypeName, ci.getId(), paramObj.toJSONString());
            throw new DeployCiJobNameLostException();
        }
        DeployJobVo deployJobVo = getBatchDeployJobVo(ci, deployVersion != null ? deployVersion.getId() : null);
        deployBatchJobService.creatBatchJob(deployJobVo, pipeline, false);
        deployJobMapper.insertJobInvoke(deployJobVo.getId(), pipelineId, JobSource.PIPELINE.getValue());

        //补充定时执行逻辑
        if (Objects.equals(deployJobVo.getTriggerType(), JobTriggerType.AUTO.getValue())) {
            IJob jobHandler = SchedulerManager.getHandler(DeployBatchJobAutoFireJob.class.getName());
            if (jobHandler == null) {
                throw new ScheduleHandlerNotFoundException(DeployBatchJobAutoFireJob.class.getName());
            }
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(deployJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
            jobHandler.reloadJob(jobObjectBuilder.build());
        }
        return deployJobVo.getId();
    }

    /**
     * 计算触发时间
     *
     * @param triggerTimeStr
     * @return
     */
    private Date getTriggerTime(String triggerTimeStr) {
        Date triggerTime = null;
        if (StringUtils.isNotBlank(triggerTimeStr)) {
            LocalTime triggerInstance = LocalTime.parse(triggerTimeStr, DateTimeFormatter.ofPattern(TimeUtil.HH_MM_SS));
            // 如果当前时间在前，则触发时间为当天；如果当前时间在后，则触发时间为第二日
            String day;
            if (LocalTime.now().isBefore(triggerInstance)) {
                day = LocalDate.now().format(DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD));
            } else {
                day = LocalDate.now().plusDays(1L).format(DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD));
            }
            triggerTime = Date.from(LocalDateTime.parse(day + " " + triggerTimeStr
                    , DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD_HH_MM_SS)).atZone(ZoneId.systemDefault()).toInstant());
        }
        return triggerTime;
    }

    /**
     * 构造批量作业VO
     *
     * @param ci              持续集成配置
     * @param deployVersionId 版本ID
     * @return
     */
    private DeployJobVo getBatchDeployJobVo(DeployCiVo ci, Long deployVersionId) {
        DeployJobVo deployJobVo = new DeployJobVo();
        deployJobVo.setPipelineId(ci.getConfig().getLong("pipelineId"));
        deployJobVo.setName(ci.getConfig().getString("jobName"));
        Date triggerTime;
        String triggerType = ci.getTriggerType();
        // 如果是立即执行，则触发时间为当前时间延后两分钟
        if (DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType())) {
            triggerType = DeployCiTriggerType.AUTO.getValue();
            triggerTime = Date.from(LocalDateTime.now().plusMinutes(2L).atZone(ZoneId.systemDefault()).toInstant());
        } else {
            triggerTime = getTriggerTime(ci.getTriggerTime());
        }
        if (DeployCiTriggerType.MANUAL.getValue().equals(triggerType)) {
            deployJobVo.setStatus(JobStatus.PENDING.getValue());
            deployJobVo.setTriggerType(JobTriggerType.MANUAL.getValue());
        } else {
            deployJobVo.setStatus(JobStatus.READY.getValue());
            deployJobVo.setTriggerType(JobTriggerType.AUTO.getValue());
            deployJobVo.setPlanStartTime(triggerTime);
        }
        deployJobVo.setAppSystemModuleVersionList(Collections.singletonList(new DeploySystemModuleVersionVo(ci.getAppSystemId(), ci.getAppModuleId(), deployVersionId)));
        deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
        deployJobVo.setSource(JobSource.BATCHDEPLOY.getValue());
        deployJobVo.setExecUser(UserContext.get().getUserUuid());
        return deployJobVo;
    }

}
