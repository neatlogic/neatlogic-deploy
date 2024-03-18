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

package neatlogic.module.deploy.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleSearchVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import neatlogic.module.deploy.service.DeployBatchJobService;
import neatlogic.module.deploy.service.DeployJobService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Component
@DisallowConcurrentExecution
public class DeployJobScheduleJob  extends JobBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private DeployPipelineMapper deployPipelineMapper;
    @Resource
    private DeployJobMapper deployJobMapper;
    @Resource
    private DeployJobService deployJobService;
    @Resource
    private DeployBatchJobService deployBatchJobService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-DEPLOY-JOB-SCHEDULE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        String uuid = jobObject.getJobName();
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleByUuid(uuid);
        if (scheduleVo == null) {
            return false;
        }
        return Objects.equals(scheduleVo.getIsActive(), 1) && Objects.equals(scheduleVo.getCron(), jobObject.getCron());
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
        DeployScheduleSearchVo searchVo = new DeployScheduleSearchVo();
        searchVo.setIsActive(1);
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
            return;
        }
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployJobVo.setSource(JobSource.DEPLOY_SCHEDULE_GENERAL.getValue());
            List<DeployJobModuleVo> moduleList = deployJobVo.getModuleList();
            deployJobService.createJobAndFire(deployJobVo, moduleList.get(0));
        } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
            PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(scheduleVo.getPipelineId());
            if (pipelineVo == null) {
                schedulerManager.unloadJob(jobObject);
                return;
            }
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployJobVo.setSource(JobSource.DEPLOY_SCHEDULE_PIPELINE.getValue());
            deployJobVo.setName("定时作业/" + pipelineVo.getName());
            deployBatchJobService.creatBatchJob(deployJobVo, pipelineVo, true);
        }
    }

    private DeployJobVo convertDeployScheduleVoToDeployJobVo(DeployScheduleVo scheduleVo) {
        DeployJobVo deployJobVo = new DeployJobVo();
        DeployScheduleConfigVo config = scheduleVo.getConfig();
        deployJobVo.setScenarioId(config.getScenarioId());
        deployJobVo.setModuleList(config.getModuleList());
        deployJobVo.setEnvId(config.getEnvId());
        deployJobVo.setParam(config.getParam());
        deployJobVo.setInvokeId(scheduleVo.getId());
        deployJobVo.setRouteId(scheduleVo.getId().toString());
        deployJobVo.setRoundCount(config.getRoundCount());
        deployJobVo.setPipelineId(scheduleVo.getPipelineId());
        deployJobVo.setAppSystemModuleVersionList(config.getAppSystemModuleVersionList());
        deployJobVo.setAppSystemId(scheduleVo.getAppSystemId());
        deployJobVo.setAppModuleId(scheduleVo.getAppModuleId());
        deployJobVo.setStatus(JobStatus.READY.getValue());
        deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
        return deployJobVo;
    }
}
