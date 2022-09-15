/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import codedriver.module.deploy.service.DeployBatchJobService;
import codedriver.module.deploy.service.DeployJobService;
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
    @Resource
    private PipelineMapper pipelineMapper;
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
        String schemaName = TenantContext.get().getDataDbName();
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
            AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId(), schemaName);
            if (appSystemVo != null) {
                scheduleVo.setAppSystemName(appSystemVo.getName());
                scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
            }
            AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(scheduleVo.getAppModuleId(), schemaName);
            if (appModuleVo != null) {
                scheduleVo.setAppModuleName(appModuleVo.getName());
                scheduleVo.setAppModuleAbbrName(appModuleVo.getAbbrName());
            }
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceVo resourceVo = resourceCrossoverMapper.getAppEnvById(config.getEnvId(), schemaName);
            if (resourceVo != null) {
                config.setEnvName(resourceVo.getName());
            }
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployJobService.createJobAndFire(deployJobVo);
        } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
            PipelineVo pipelineVo = pipelineMapper.getPipelineById(scheduleVo.getPipelineId());
            if (pipelineVo == null) {
                schedulerManager.unloadJob(jobObject);
                return;
            }
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployBatchJobService.creatBatchJob(deployJobVo, pipelineVo, true);
            deployJobMapper.insertJobInvoke(deployJobVo.getId(), deployJobVo.getInvokeId(), deployJobVo.getSource());
        }
    }

    private DeployJobVo convertDeployScheduleVoToDeployJobVo(DeployScheduleVo scheduleVo) {
        DeployJobVo deployJobVo = new DeployJobVo();
        DeployScheduleConfigVo config = scheduleVo.getConfig();
        deployJobVo.setScenarioId(config.getScenarioId());
        deployJobVo.setModuleList(config.getModuleList());
        deployJobVo.setEnvId(config.getEnvId());
        deployJobVo.setEnvName(config.getEnvName());
        deployJobVo.setParam(config.getParam());
        deployJobVo.setSource(JobSource.DEPLOYSCHEDULE.getValue());
        deployJobVo.setInvokeId(scheduleVo.getId());
        deployJobVo.setRoundCount(config.getRoundCount());
        deployJobVo.setPipelineId(scheduleVo.getPipelineId());
        deployJobVo.setAppSystemModuleVersionList(config.getAppSystemModuleVersionList());
        deployJobVo.setAppSystemId(scheduleVo.getAppSystemId());
        deployJobVo.setAppSystemName(scheduleVo.getAppSystemName());
        deployJobVo.setAppSystemAbbrName(scheduleVo.getAppSystemAbbrName());
        deployJobVo.setAppModuleId(scheduleVo.getAppModuleId());
        deployJobVo.setAppModuleName(scheduleVo.getAppModuleName());
        deployJobVo.setAppModuleAbbrName(scheduleVo.getAppModuleAbbrName());
        deployJobVo.setStatus(JobStatus.READY.getValue());
        deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
        return deployJobVo;
    }
}
