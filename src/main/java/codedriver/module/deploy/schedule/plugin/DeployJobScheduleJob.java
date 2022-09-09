/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@DisallowConcurrentExecution
public class DeployJobScheduleJob  extends JobBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-DEPLOY-JOB-SCHEDULE-JOB";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        String uuid = jobObject.getJobName();
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleByUuid(uuid);
        if (scheduleVo != null) {
            JobObject newJobObjectBuilder = new JobObject.Builder(scheduleVo.getUuid(), this.getGroupName(), this.getClassName(), tenantUuid)
                    .withCron(scheduleVo.getCron()).withBeginTime(scheduleVo.getBeginTime())
                    .withEndTime(scheduleVo.getEndTime())
                    .build();
            schedulerManager.loadJob(newJobObjectBuilder);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        DeployScheduleVo searchVo = new DeployScheduleVo();
        int rowNum = deployScheduleMapper.getScheduleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<DeployScheduleVo> list = deployScheduleMapper.getScheduleList(searchVo);
                for (DeployScheduleVo scheduleVo : list) {
                    JobObject.Builder jobObjectBuilder = new JobObject
                            .Builder(scheduleVo.getUuid(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
                    JobObject jobObject = jobObjectBuilder.build();
                    this.reloadJob(jobObject);
                }
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        String uuid = jobObject.getJobName();
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleByUuid(uuid);
        if (scheduleVo == null) {
            schedulerManager.unloadJob(jobObject);
        }
        // TODO
    }
}
