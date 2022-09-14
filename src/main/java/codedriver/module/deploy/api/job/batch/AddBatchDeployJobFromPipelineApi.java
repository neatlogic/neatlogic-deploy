/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import codedriver.framework.deploy.auth.BATCHDEPLOY_VERIFY;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.job.DeployJobAuthVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.framework.deploy.dto.job.LaneVo;
import codedriver.framework.deploy.dto.pipeline.*;
import codedriver.framework.deploy.exception.DeployJobParamIrregularException;
import codedriver.framework.deploy.exception.DeployPipelineNotFoundException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import codedriver.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import codedriver.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = BATCHDEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class AddBatchDeployJobFromPipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private PipelineMapper pipelineMapper;

    @Resource
    private DeployJobService deployJobService;


    @Override
    public String getName() {
        return "通过超级流水线添加批量发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/addbatchjob";
    }

    @Input({@Param(name = "pipelineId", type = ApiParamType.LONG, isRequired = true, desc = "超级流水线id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名称"),
            @Param(name = "appSystemModuleVersionList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "选中的系统模块和版本，数组对象需要包含appSystemId,appModuleId和versionId三个字段"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "manual,auto", desc = "触发方式"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划开始时间")})
    @Output({@Param(explode = DeployJobVo.class)})
    @ResubmitInterval(3)
    @Description(desc = "通过超级流水线添加批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray appSystemModuleVersionList = jsonObj.getJSONArray("appSystemModuleVersionList");
        if (CollectionUtils.isEmpty(appSystemModuleVersionList)) {
            throw new DeployJobParamIrregularException("应用模块版本列表");
        }
        Long pipelineId = jsonObj.getLong("pipelineId");
        PipelineVo pipelineVo = pipelineMapper.getPipelineById(pipelineId);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundException(pipelineId);
        }
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        if (deployJobVo.getTriggerType().equals(JobTriggerType.AUTO.getValue())) {
            if (deployJobVo.getPlanStartTime() == null) {
                throw new DeployJobParamIrregularException("计划开始时间");
            }
            deployJobVo.setStatus(JobStatus.READY.getValue());
        } else if (deployJobVo.getTriggerType().equals(JobTriggerType.MANUAL.getValue())) {
            deployJobVo.setStatus(JobStatus.PENDING.getValue());
            deployJobVo.setPlanStartTime(null);
        }
        if (!AuthActionChecker.check(BATCHDEPLOY_VERIFY.class)) {
            deployJobVo.setReviewStatus(ReviewStatus.WAITING.getValue());
        } else {
            deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
        }
        deployJobVo.setSource(JobSource.BATCHDEPLOY.getValue());
        deployJobVo.setExecUser(UserContext.get().getUserUuid());

        if (CollectionUtils.isNotEmpty(pipelineVo.getLaneList())) {
            for (int i = 0; i < pipelineVo.getLaneList().size(); i++) {
                PipelineLaneVo pipelineLaneVo = pipelineVo.getLaneList().get(i);
                LaneVo laneVo = new LaneVo();
                boolean hasLaneJob = false;
                if (CollectionUtils.isNotEmpty(pipelineLaneVo.getGroupList())) {
                    for (int j = 0; j < pipelineLaneVo.getGroupList().size(); j++) {
                        PipelineGroupVo pipelineGroupVo = pipelineLaneVo.getGroupList().get(j);
                        LaneGroupVo groupVo = new LaneGroupVo();
                        groupVo.setNeedWait(pipelineGroupVo.getNeedWait());
                        boolean hasGroupJob = false;
                        if (CollectionUtils.isNotEmpty(pipelineGroupVo.getJobTemplateList())) {
                            for (int k = 0; k < pipelineGroupVo.getJobTemplateList().size(); k++) {
                                PipelineJobTemplateVo jobTemplateVo = pipelineGroupVo.getJobTemplateList().get(k);
                                Long versionId = getVersionId(appSystemModuleVersionList, jobTemplateVo);
                                if (versionId != null) {
                                    hasLaneJob = true;
                                    hasGroupJob = true;
                                    DeployJobVo jobVo = new DeployJobVo();
                                    jobVo.setAppSystemId(jobTemplateVo.getAppSystemId());
                                    jobVo.setAppModuleId(jobTemplateVo.getAppModuleId());
                                    jobVo.setScenarioId(jobTemplateVo.getScenarioId());
                                    jobVo.setEnvId(jobTemplateVo.getEnvId());
                                    jobVo.setVersionId(versionId);
                                    deployJobService.createJob(jobVo);
                                    deployJobMapper.insertGroupJob(groupVo.getId(), jobVo.getId(), k + 1);
                                    deployJobMapper.insertJobInvoke(deployJobVo.getId(), jobVo.getId(), JobSource.BATCHDEPLOY.getValue());
                                    jobVo.setParentId(deployJobVo.getId());
                                    deployJobMapper.updateAutoExecJobParentIdById(jobVo);
                                }
                            }
                        }
                        if (hasGroupJob) {
                            groupVo.setLaneId(laneVo.getId());
                            groupVo.setSort(j + 1);
                            groupVo.setStatus(JobStatus.PENDING.getValue());
                            deployJobMapper.insertLaneGroup(groupVo);
                        }
                    }
                }
                if (hasLaneJob) {
                    laneVo.setBatchJobId(deployJobVo.getId());
                    laneVo.setSort(i + 1);
                    laneVo.setStatus(JobStatus.PENDING.getValue());
                    deployJobMapper.insertLane(laneVo);
                }
            }
        }

        deployJobMapper.insertAutoExecJob(deployJobVo);
        deployJobMapper.insertJobInvoke(deployJobVo.getId(), pipelineId, JobSource.PIPELINE.getValue());
        if (CollectionUtils.isNotEmpty(pipelineVo.getAuthList())) {
            for (PipelineAuthVo authVo : pipelineVo.getAuthList()) {
                DeployJobAuthVo deployAuthVo = new DeployJobAuthVo();
                deployAuthVo.setJobId(deployJobVo.getId());
                deployAuthVo.setAuthUuid(authVo.getAuthUuid());
                deployAuthVo.setType(authVo.getType());
                deployJobMapper.insertDeployJobAuth(deployAuthVo);
            }
        }

        //补充定时执行逻辑
        if (Objects.equals(deployJobVo.getTriggerType(), JobTriggerType.AUTO.getValue())) {
            if (!jsonObj.containsKey("planStartTime")) {
                throw new ParamIrregularException("planStartTime");
            }
            IJob jobHandler = SchedulerManager.getHandler(DeployBatchJobAutoFireJob.class.getName());
            if (jobHandler == null) {
                throw new ScheduleHandlerNotFoundException(DeployBatchJobAutoFireJob.class.getName());
            }
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(deployJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
            jobHandler.reloadJob(jobObjectBuilder.build());
        }
        return deployJobMapper.getBatchDeployJobById(deployJobVo.getId());
    }


    static Long getVersionId(JSONArray appSystemModuleVersionList, PipelineJobTemplateVo jobTemplateVo) {
        for (int i = 0; i < appSystemModuleVersionList.size(); i++) {
            JSONObject dataObj = appSystemModuleVersionList.getJSONObject(i);
            if (dataObj.getLong("appSystemId").equals(jobTemplateVo.getAppSystemId()) && dataObj.getLong("appModuleId").equals(jobTemplateVo.getAppModuleId())) {
                return dataObj.getLong("versionId");
            }
        }
        return null;
    }
}
