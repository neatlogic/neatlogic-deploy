/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
