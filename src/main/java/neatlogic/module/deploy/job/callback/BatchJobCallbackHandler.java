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

package neatlogic.module.deploy.job.callback;

import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.module.deploy.service.DeployBatchJobService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/7/27 17:40
 **/
@Transactional
@Component
public class BatchJobCallbackHandler extends AutoexecJobCallbackBase {
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private DeployBatchJobService deployBatchJobService;

    @Override
    public String getHandler() {
        return BatchJobCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        if (jobVo != null) {
            AutoexecJobVo autoexecJob = autoexecJobMapper.getJobInfo(jobVo.getId());
            if (Arrays.asList(JobSource.BATCHDEPLOY.getValue(), JobSource.DEPLOY_SCHEDULE_PIPELINE.getValue(), JobSource.DEPLOY.getValue()).contains(autoexecJob.getSource())
                    && autoexecJob.getParentId() != null && autoexecJob.getParentId() != -1) {
                //作业回调
                AutoexecJobVo parentJobVo = autoexecJobMapper.getJobInfo(autoexecJob.getParentId());
                if (parentJobVo != null && Arrays.asList(JobSource.BATCHDEPLOY.getValue(), JobSource.DEPLOY_SCHEDULE_PIPELINE.getValue()).contains(parentJobVo.getSource())) {
                    if (JobStatus.RUNNING.getValue().equals(autoexecJob.getStatus())) {
                        AutoexecJobVo autoexecParentJobVo = new AutoexecJobVo();
                        autoexecParentJobVo.setId(autoexecJob.getParentId());
                        autoexecParentJobVo.setStatus(autoexecJob.getStatus());
                        autoexecJobMapper.updateJobStatus(autoexecParentJobVo);
                    }
                    return Arrays.asList(JobStatus.COMPLETED.getValue(), JobStatus.FAILED.getValue(), JobStatus.ABORTED.getValue()).contains(autoexecJob.getStatus());
                }
            } else if (Objects.equals(autoexecJob.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                //TODO 批量作业回调
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        AutoexecJobVo autoexecJob = autoexecJobMapper.getJobInfo(jobVo.getId());
        autoexecJobMapper.getJobLockByJobId(autoexecJob.getParentId());
        deployBatchJobService.checkAndFireLaneNextGroupByJobId(jobVo.getId(), jobVo.getPassThroughEnv());
    }
}
