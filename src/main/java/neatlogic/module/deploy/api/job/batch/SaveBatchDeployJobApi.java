/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.job.batch;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_VERIFY;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.job.DeployJobAuthVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.dto.job.LaneVo;
import neatlogic.framework.deploy.exception.DeployBatchJobCannotEditException;
import neatlogic.framework.deploy.exception.DeployBatchJobNotFoundException;
import neatlogic.framework.deploy.exception.DeployJobHasParentException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import neatlogic.module.deploy.service.DeployJobService;
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
public class SaveBatchDeployJobApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployJobService deployJobService;


    @Override
    public String getName() {
        return "保存批量发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/save";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "作业id，不提供代表添加作业"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名称"),
            @Param(name = "saveMode", type = ApiParamType.ENUM, rule = "save,commit", isRequired = true, desc = "暂存或提交"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "manual,auto", desc = "触发方式"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划开始时间"),
            @Param(name = "laneList", type = ApiParamType.JSONARRAY, desc = "通道列表"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表")})
    @Output({@Param(explode = DeployJobVo.class)})
    @ResubmitInterval(3)
    @Description(desc = "保存批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (id != null) {
            DeployJobVo deployJobVo = deployJobMapper.getBatchDeployJobById(id);
            if (deployJobVo == null) {
                throw new DeployBatchJobNotFoundException(id);
            }
            //如果批量作业是running状态则不允许编辑
            if (Objects.equals(JobStatus.RUNNING.getValue(), deployJobVo.getStatus())) {
                throw new DeployBatchJobCannotEditException(JobStatus.RUNNING.getText());
            }
        }
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        String saveMode = jsonObj.getString("saveMode");
        if (saveMode.equals("save")) {
            deployJobVo.setStatus(JobStatus.SAVED.getValue());
            deployJobVo.setReviewStatus(null);
        } else {
            if (Objects.equals(deployJobVo.getTriggerType(), JobTriggerType.AUTO.getValue())){
                deployJobVo.setStatus(JobStatus.READY.getValue());
            }else{
                deployJobVo.setStatus(JobStatus.PENDING.getValue());
            }
            if (!AuthActionChecker.check(BATCHDEPLOY_VERIFY.class)) {
                deployJobVo.setReviewStatus(ReviewStatus.WAITING.getValue());
            } else {
                deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
            }
        }
        deployJobVo.setSource(JobSource.BATCHDEPLOY.getValue());
        deployJobVo.setParentId(-1L);
        deployJobVo.setExecUser(UserContext.get().getUserUuid());
        if (id == null) {
            deployJobMapper.insertAutoExecJob(deployJobVo);
        } else {
            deployJobMapper.updateAutoExecJob(deployJobVo);
            deployJobMapper.deleteLaneGroupJobByJobId(deployJobVo.getId());
            deployJobMapper.resetAutoexecJobParentId(deployJobVo.getId());
            deployJobMapper.deleteJobInvokeByJobId(deployJobVo.getId());
            deployJobMapper.deleteJobAuthByJobId(deployJobVo.getId());
        }
        if (CollectionUtils.isNotEmpty(deployJobVo.getLaneList())) {
            for (int i = 0; i < deployJobVo.getLaneList().size(); i++) {
                LaneVo laneVo = deployJobVo.getLaneList().get(i);
                boolean hasLaneJob = false;
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (int j = 0; j < laneVo.getGroupList().size(); j++) {
                        LaneGroupVo groupVo = laneVo.getGroupList().get(j);
                        boolean hasGroupJob = false;
                        if (CollectionUtils.isNotEmpty(groupVo.getJobList())) {
                            hasLaneJob = true;
                            hasGroupJob = true;
                            for (int k = 0; k < groupVo.getJobList().size(); k++) {
                                DeployJobVo jobVo = groupVo.getJobList().get(k);
                                DeployJobVo checkJobVo = deployJobMapper.getJobBaseInfoById(jobVo.getId());
                                if (checkJobVo.getParentId() != null && !checkJobVo.getParentId().equals(deployJobVo.getId())) {
                                    throw new DeployJobHasParentException(checkJobVo.getName());
                                }
                                jobVo.setParentId(deployJobVo.getId());
                                deployJobMapper.updateAutoExecJobParentIdById(jobVo);
                                deployJobMapper.insertGroupJob(groupVo.getId(), jobVo.getId(), k + 1);
                                if (deployJobVo.getRouteId() == null) {
                                    System.out.println("2");
                                }

                                System.out.println("c=" + deployJobVo.getId());
                                deployJobMapper.insertJobInvoke(deployJobVo.getId(), jobVo.getId(), JobSource.BATCHDEPLOY.getValue(), deployJobVo.getRouteId());
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
        if (CollectionUtils.isNotEmpty(deployJobVo.getAuthList())) {
            for (DeployJobAuthVo authVo : deployJobVo.getAuthList()) {
                authVo.setJobId(deployJobVo.getId());
                deployJobMapper.insertDeployJobAuth(authVo);
            }
        }
        //补充定时执行逻辑
        if (saveMode.equals("commit") && Objects.equals(deployJobVo.getTriggerType(), JobTriggerType.AUTO.getValue())) {
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

}
