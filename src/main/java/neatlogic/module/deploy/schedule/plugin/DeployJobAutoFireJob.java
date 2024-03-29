/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.service.AuthenticationInfoService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 在组合工具保存的作业，设置为自动触发后，创建本Job，到达计划时间后自动执行作业
 *
 * @author lvzk
 * @since 2022/7/18 17:42
 **/
@Component
@DisallowConcurrentExecution
public class DeployJobAutoFireJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(DeployJobAutoFireJob.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-DEPLOY-JOBAUTOFIRE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(Long.valueOf(jobObject.getJobName()));
        return jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType());
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        Long jobId = Long.valueOf(jobObject.getJobName());
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType())) {
            try {
                if (jobVo.getPlanStartTime().after(new Date())) {
                    JobObject.Builder newJobObjectBuilder = new JobObject
                            .Builder(jobId.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid())
                            .withBeginTime(jobVo.getPlanStartTime())
                            .withIntervalInSeconds(60 * 60)
                            .withRepeatCount(0);
                    JobObject newJobObject = newJobObjectBuilder.build();
                    schedulerManager.loadJob(newJobObject);
                } else {
                    fireJob(jobVo);
                }
            } catch (Exception ex) {
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        List<Long> list = autoexecJobMapper.getJobIdListByStatusAndTriggerTypeWithoutBatch(JobStatus.READY.getValue(), JobTriggerType.AUTO.getValue());
        for (Long id : list) {
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(id.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
            JobObject jobObject = jobObjectBuilder.build();
            this.reloadJob(jobObject);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType())) {
            fireJob(jobVo);
        }
        schedulerManager.unloadJob(jobObject);
    }

    private void fireJob(AutoexecJobVo jobVo) throws Exception {
        UserVo execUser;
        if(Objects.equals(jobVo.getExecUser(),SystemUser.SYSTEM.getUserUuid())){
            execUser = SystemUser.SYSTEM.getUserVo();
        }else {
            execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        }
        if (execUser != null) {
            AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(execUser.getUuid());
            UserContext.init(execUser, authenticationInfo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
            IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
            autoexecJobActionCrossoverService.getJobDetailAndFireJob(jobVo);
        }
    }

}
