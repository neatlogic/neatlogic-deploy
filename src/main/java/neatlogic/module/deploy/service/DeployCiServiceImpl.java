package neatlogic.module.deploy.service;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.util.StringUtil;
import neatlogic.framework.deploy.constvalue.DeployCiGitlabAuthMode;
import neatlogic.framework.deploy.constvalue.DeployCiRepoType;
import neatlogic.framework.deploy.constvalue.DeployCiTriggerType;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.dto.version.DeploySystemModuleVersionVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
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
        if (Objects.equals(DeployCiTriggerType.DELAY.getValue(), ci.getTriggerType())) {
            triggerTime = Date.from(LocalDateTime.now().plusSeconds(ci.getDelayTime()).atZone(ZoneId.systemDefault()).toInstant());
        }
        if (Arrays.asList(DeployCiTriggerType.INSTANT.getValue(), DeployCiTriggerType.DELAY.getValue()).contains(ci.getTriggerType())) {
            triggerType = DeployCiTriggerType.AUTO.getValue();
        }
        DeployJobVo deployJobParam = new DeployJobVo(ci.getAppSystemId(), scenarioId, envId, triggerType, triggerTime, ci.getConfig().getInteger("roundCount"), ci.getConfig().getJSONObject("param"));
        JSONArray selectNodeList = ci.getConfig().getJSONArray("selectNodeList");
        DeployJobModuleVo moduleVo = new DeployJobModuleVo(ci.getAppModuleId(), deployVersion != null ? deployVersion.getVersion() : null, CollectionUtils.isNotEmpty(selectNodeList) ? selectNodeList.toJavaList(AutoexecNodeVo.class) : new ArrayList<>());
        // 包含编译工具则新建buildNo
        if (Objects.equals(scenarioVo.getIsHasBuildTypeTool(), 1)) {
            moduleVo.setBuildNo(-1);
        }
        deployJobParam.setModuleList(Collections.singletonList(moduleVo));
        deployJobParam.setSource(JobSource.DEPLOY_CI.getValue());
        deployJobParam.setInvokeId(ci.getId());
        if (!Objects.equals(ci.getTriggerType(), DeployCiTriggerType.INSTANT.getValue())) {
            deployJobService.createJobAndSchedule(deployJobParam, moduleVo);
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
        if (deployJobVo.getRouteId() == null) {
            System.out.println("4");
        }
        deployJobMapper.insertJobInvoke(deployJobVo.getId(), pipelineId, JobSource.PIPELINE.getValue(), deployJobVo.getRouteId());

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

    @Override
    public void deleteGitlabWebHook(DeployCiVo ci, String runnerUrl) {
        String gitlabUsername = null;
        String gitlabPassword = null;
        JSONObject config = ci.getConfig();
        if (config != null) {
            gitlabUsername = config.getString("gitlabUsername");
            gitlabPassword = config.getString("gitlabPassword");
        }
        if (StringUtils.isBlank(gitlabUsername) || StringUtils.isBlank(gitlabPassword)) {
            throw new DeployCiGitlabAccountLostException(ci.getRepoServerAddress(), ci.getRepoName());
        }
        JSONObject param = new JSONObject();
        param.put("hookId", ci.getHookId());
        param.put("repoServerAddress", ci.getRepoServerAddress());
        param.put("repoName", ci.getRepoName());
        param.put("authMode", DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue());
        param.put("username", gitlabUsername);
        param.put("password", gitlabPassword);
        String url = runnerUrl + "/api/rest/deploy/ci/gitlabwebhook/delete";
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(param.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        String errorMsg = request.getErrorMsg();
        if (StringUtils.isNotBlank(errorMsg)) {
            logger.error("Gitlab webhook delete failed. Request url: {}; params: {}; errorMsg: {}", url, param.toJSONString(), errorMsg);
            throw new DeployCiGitlabWebHookDeleteFailedException(ci.getRepoServerAddress(), ci.getRepoName());
        }
    }
}
