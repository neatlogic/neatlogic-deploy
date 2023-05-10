/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.deploy.job.callback;

import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.module.deploy.service.DeployBatchJobService;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/7/27 17:40
 **/
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
            if (Objects.equals(JobSource.DEPLOY.getValue(), autoexecJob.getSource()) && autoexecJob.getParentId() != null) {
                //作业回调
                AutoexecJobVo parentJobVo = autoexecJobMapper.getJobInfo(autoexecJob.getParentId());
                if (MapUtils.isNotEmpty(jobVo.getPassThroughEnv()) && jobVo.getPassThroughEnv().containsKey("DEPLOY_ID_PATH") && parentJobVo != null && Objects.equals(parentJobVo.getSource(), JobSource.BATCHDEPLOY.getValue())) {
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
        deployBatchJobService.checkAndFireLaneNextGroupByJobId(jobVo.getId(),jobVo.getPassThroughEnv());
    }
}
