/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.callback;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployJobTriggerMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/9/19 17:40
 **/
@Component
public class DeployJobTriggerCallbackHandler extends AutoexecJobCallbackBase {
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private DeployJobTriggerMapper triggerMapper;
    @Resource
    private DeployJobMapper deployJobMapper;

    @Override
    public String getHandler() {
        return DeployJobTriggerCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        if (jobVo != null) {
            AutoexecJobVo autoexecJob = autoexecJobMapper.getJobInfo(jobVo.getId());
            //普通作业触发器
            if (Objects.equals(JobSource.DEPLOY.getValue(), autoexecJob.getSource()) && autoexecJob.getParentId() == null) {
                //作业回调
//                DeployJobVo deployJobVo =
//                List<DeployJobTriggerVo> triggerVoList = triggerMapper.getTriggerListByAppSystemIdAndAppModuleId();
            } else if (Objects.equals(autoexecJob.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                //TODO 批量作业回调
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {

    }
}
