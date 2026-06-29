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

package neatlogic.module.deploy.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleSearchVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.dto.JobVo;
import neatlogic.framework.scheduler.enums.JobLoadTriggerType;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import neatlogic.module.deploy.service.DeployBatchJobService;
import neatlogic.module.deploy.service.DeployJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Component
@DisallowConcurrentExecution
public class DeployJobScheduleJob extends JobBase {
    @Override
    public String getName() {
        return "发布定时作业执行";
    }

    static Logger logger = LoggerFactory.getLogger(DeployJobScheduleJob.class);
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
    @Resource
    private UserMapper userMapper;
    @Resource
    private AuthenticationInfoService authenticationInfoService;

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
        if (jobObject.isTest() == 1) {
            return true;
        }
        return Objects.equals(scheduleVo.getIsActive(), 1) && Objects.equals(scheduleVo.getCron(), jobObject.getCron());
    }

    @Override
    public void reloadJob(JobObject jobObject, JobLoadTriggerType triggerType) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        String uuid = jobObject.getJobName();
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleByUuid(uuid);
        if (scheduleVo != null) {
            JobObject newJobObjectBuilder = new JobObject.Builder(scheduleVo.getUuid(), this.getGroupName(), this.getClassName(), tenantUuid)
                    .withCron(scheduleVo.getCron()).withBeginTime(scheduleVo.getBeginTime())
                    .withEndTime(scheduleVo.getEndTime())
                    .build();
            schedulerManager.loadJob(newJobObjectBuilder, triggerType);
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
                List<Long> idList = deployScheduleMapper.getScheduleIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    List<DeployScheduleVo> list = deployScheduleMapper.getScheduleListByIdList(idList);
                    for (DeployScheduleVo scheduleVo : list) {
                        JobObject.Builder jobObjectBuilder = new JobObject
                                .Builder(scheduleVo.getUuid(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
                        JobObject jobObject = jobObjectBuilder.build();
                        this.reloadJob(jobObject, JobLoadTriggerType.SERVER_RESTART);
                    }
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
        String execUserUuid = scheduleVo.getLcu();
        if(jobObject.isTest() == 1 && StringUtils.isNotBlank(jobObject.getTestUserUuid())){
            execUserUuid = jobObject.getTestUserUuid();
        }
        UserVo execUser = userMapper.getUserBaseInfoByUuid(execUserUuid);
        if (execUser == null) {
            schedulerManager.unloadJob(jobObject);
            logger.error("execUser: {} not exist!", execUserUuid);
            return;
        }
        AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(execUser.getUuid());
        UserContext.init(execUser, authenticationInfo, SystemUser.SYSTEM.getTimezone());
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployJobVo.setSource(JobSource.DEPLOY_SCHEDULE_GENERAL.getValue());
            List<DeployJobModuleVo> moduleList = deployJobVo.getModuleList();
            deployJobService.createJobAndFire(deployJobVo, moduleList.get(0));
        } else if (type.equals(ScheduleType.PIPELINE.getValue())) {
            PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(scheduleVo.getPipelineId());
            if (pipelineVo == null) {
                schedulerManager.unloadJob(jobObject);
                return;
            }
            DeployJobVo deployJobVo = convertDeployScheduleVoToDeployJobVo(scheduleVo);
            deployJobVo.setSource(JobSource.DEPLOY_SCHEDULE_PIPELINE.getValue());
            deployJobVo.setName("定时作业/" + pipelineVo.getName());
            deployBatchJobService.creatBatchJob(deployJobVo, pipelineVo);
            deployBatchJobService.fireBatch(deployJobVo.getId(), JobAction.RESET_REFIRE.getValue(), JobAction.RESET_REFIRE.getValue());
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
        deployJobVo.setParallelPolicy(config.getParallelPolicy());
        deployJobVo.setRoundCount(config.getRoundCount());
        deployJobVo.setParallelCount(config.getParallelCount());
        deployJobVo.setPipelineId(scheduleVo.getPipelineId());
        deployJobVo.setAppSystemModuleVersionList(config.getAppSystemModuleVersionList());
        deployJobVo.setAppSystemId(scheduleVo.getAppSystemId());
        deployJobVo.setAppModuleId(scheduleVo.getAppModuleId());
        deployJobVo.setStatus(JobStatus.READY.getValue());
        deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
        return deployJobVo;
    }

    @Override
    public JobVo getJob(String uuid) {
        JobVo jobVo = null;
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleByUuid(uuid);
        if (scheduleVo != null) {
            deployJobService.scheduleAuthCheck(scheduleVo);
            jobVo = new JobVo();
            jobVo.setName(scheduleVo.getName());
            jobVo.setUuid(scheduleVo.getUuid());
            jobVo.setCron(scheduleVo.getCron());
            jobVo.setIsActive(scheduleVo.getIsActive());
        }
        return jobVo;
    }
}
