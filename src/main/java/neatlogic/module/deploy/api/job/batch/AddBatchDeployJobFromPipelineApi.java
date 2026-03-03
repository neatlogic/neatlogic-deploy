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

package neatlogic.module.deploy.api.job.batch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_VERIFY;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.exception.DeployJobParamIrregularException;
import neatlogic.framework.deploy.exception.DeployPipelineNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import neatlogic.module.deploy.service.DeployBatchJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class AddBatchDeployJobFromPipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Resource
    private DeployBatchJobService deployBatchJobService;


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
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "manual,auto,instant", desc = "触发方式"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划开始时间")})
    @Output({@Param(explode = DeployJobVo.class)})
    @ResubmitInterval(3)
    @Description(desc = "通过超级流水线添加批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray appSystemModuleVersionList = jsonObj.getJSONArray("appSystemModuleVersionList");
        if (CollectionUtils.isEmpty(appSystemModuleVersionList)) {
            throw new DeployJobParamIrregularException("appSystemModuleVersionList");
        }
        Long pipelineId = jsonObj.getLong("pipelineId");
        PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(pipelineId);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundException(pipelineId);
        }
        DeployJobVo deployJobVo = JSON.toJavaObject(jsonObj, DeployJobVo.class);
        boolean isInstantExecute = Objects.equals("instant", deployJobVo.getTriggerType());
        if (deployJobVo.getTriggerType().equals(JobTriggerType.AUTO.getValue())) {
            if (deployJobVo.getPlanStartTime() == null) {
                throw new DeployJobParamIrregularException("planStartTime");
            }
            deployJobVo.setStatus(JobStatus.READY.getValue());
        } else if (isInstantExecute) {
            deployJobVo.setStatus(JobStatus.PENDING.getValue());
            deployJobVo.setPlanStartTime(null);
            deployJobVo.setTriggerType(JobTriggerType.MANUAL.getValue());
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
        deployJobVo.setParentId(-1L);
        deployJobVo.setInvokeId(pipelineId);
        deployJobVo.setRouteId(pipelineId.toString());
        deployJobVo.setExecUser(UserContext.get().getUserUuid());
        deployBatchJobService.creatBatchJob(deployJobVo, pipelineVo);

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
        } else if (isInstantExecute && Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
            deployBatchJobService.fireBatch(deployJobVo.getId(), "refireAll", "refireAll");
        }
        return deployJobMapper.getBatchDeployJobById(deployJobVo.getId());
    }
}
