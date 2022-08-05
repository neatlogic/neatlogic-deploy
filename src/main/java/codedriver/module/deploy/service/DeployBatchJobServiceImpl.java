/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.framework.deploy.dto.job.LaneVo;
import codedriver.framework.deploy.exception.DeployBatchJobCannotExecuteException;
import codedriver.framework.deploy.exception.DeployBatchJobFireWithRevokedException;
import codedriver.framework.deploy.exception.DeployBatchJobGroupFireWithInvalidStatusException;
import codedriver.module.deploy.dao.mapper.DeployBatchJobMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class DeployBatchJobServiceImpl implements DeployBatchJobService {
    static Logger logger = LoggerFactory.getLogger(DeployBatchJobServiceImpl.class);
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public void fireBatch(Long batchJobId) {
        deployBatchJobMapper.getBatchDeployJobLockById(batchJobId);
        AutoexecJobVo batchJobVo = autoexecJobMapper.getJobInfo(batchJobId);
        //不允许存在"已撤销"的作业
        List<AutoexecJobVo> autoexecJobList = deployBatchJobMapper.getBatchDeployJobListByIdAndStatus(batchJobId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        if (CollectionUtils.isNotEmpty(autoexecJobList)) {
            throw new DeployBatchJobFireWithRevokedException(autoexecJobList);
        }
        //更新批量发布父作业状态
        String loginUserUuid = UserContext.get().getUserUuid();
        if (!Objects.equals(loginUserUuid, batchJobVo.getExecUser())) {
            throw new DeployBatchJobCannotExecuteException();
        }
        batchJobVo.setStatus(JobStatus.PENDING.getValue());
        autoexecJobMapper.updateJobStatus(batchJobVo);
        //循环泳道，获取每个泳道第一个组fire
        List<LaneVo> laneVos = deployBatchJobMapper.getLaneListByBatchJobId(batchJobVo.getId());
        for (LaneVo laneVo : laneVos) {
            Long fireGroupId = deployBatchJobMapper.getLaneFireGroupId(batchJobVo.getId(), laneVo.getId());
            deployBatchJobMapper.updateLaneStatus(laneVo.getId(), JobStatus.RUNNING.getValue());
            deployBatchJobMapper.updateGroupStatusByLaneId(laneVo.getId(), JobStatus.PENDING.getValue());
            fireLaneGroup(fireGroupId);
        }
    }

    @Override
    public void fireLaneGroup(Long groupId) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        fireLaneGroup(groupVo, false);
    }

    @Override
    public void fireLaneGroup(Long groupId, int needWait) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        groupVo.setNeedWait(needWait);
        fireLaneGroup(groupVo, false);
    }

    @Override
    public void fireLaneGroup(LaneGroupVo groupVo, boolean isRefire) {
        Long groupId = groupVo.getId();
        if (Objects.equals(groupVo.getStatus(), JobStatus.COMPLETED.getValue())) {
            logger.info("Batch run fire group:#" + groupId + " status succeed, ignore.");
            //无需等待，直接执行下一个组
            if (groupVo.getNeedWait() != 1) {
                checkAndFireLaneNextGroup(groupVo);
            }
            return;
        }

        // 如果待触发的group的状态是running，则停止触发
        if (Objects.equals(JobStatus.RUNNING.getValue(), groupVo.getStatus())) {
            if (isRefire) {
                throw new DeployBatchJobGroupFireWithInvalidStatusException(groupVo.getStatus());
            }
            return;
        }

        // 如果属于正常触发，则继续执行以下逻辑
        logger.info("Batch run fire group:#" + groupId);
        groupVo.setStatus(JobStatus.RUNNING.getValue());
        deployBatchJobMapper.updateGroupStatus(groupVo);
        deployBatchJobMapper.updateLaneStatus(groupVo.getLaneId(), JobStatus.RUNNING.getValue());

        List<AutoexecJobVo> jobVoList = deployBatchJobMapper.getJobsByGroupIdAndWithoutStatus(groupId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        if (jobVoList.size() == 0) {
            if (groupVo.getNeedWait() != 1) {
                checkAndFireLaneNextGroup(groupVo);
            }
        } else {
            //循环执行作业
            for (AutoexecJobVo jobVo : jobVoList) {
                try {
                    jobVo.setAction(groupVo.getRefireType());
                    jobVo.setAction(JobAction.RESET_REFIRE.getValue());
                    IAutoexecJobActionHandler refireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.REFIRE.getValue());
                    jobVo.setIsTakeOver(1);
                    refireAction.doService(jobVo);
                } catch (Exception ex) {
                    logger.error("Fire job by batch failed," + ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public void checkAndFireLaneNextGroup(Long groupId) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        if (groupVo != null)
            checkAndFireLaneNextGroup(groupVo);
    }

    @Override
    public void checkAndFireLaneNextGroup(LaneGroupVo groupVo) {
        Long groupId = groupVo.getId();
        List<AutoexecJobVo> groupJobs = deployBatchJobMapper.getJobsByGroupIdAndWithoutStatus(groupId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        //初始化 状态数量map
        Map<String, Integer> statusCountMap = new HashMap<>();
        for (JobPhaseStatus jobStatus : JobPhaseStatus.values()) {
            statusCountMap.put(jobStatus.getValue(), 0);
        }
        //List<Long> failedJobsId = new ArrayList<>();
        for (AutoexecJobVo jobVo : groupJobs) {
//            if (Objects.equals(JobStatus.FAILED.getValue(), jobVo.getStatus())) {
//                failedJobsId.add(jobVo.getId());
//            }
            statusCountMap.put(jobVo.getStatus(), statusCountMap.get(jobVo.getStatus()) + 1);
        }
        //根据状态数量map 获取最终组状态
        String groupStatus = groupVo.getStatus();
        if (statusCountMap.get(JobStatus.RUNNING.getValue()) > 0) {
            groupStatus = JobStatus.RUNNING.getValue();
        } else if (statusCountMap.get(JobStatus.PENDING.getValue()) > 0) {
            if (Objects.equals(JobStatus.RUNNING.getValue(), groupVo.getStatus())) {
                groupStatus = JobStatus.RUNNING.getValue();
            } else {
                groupStatus = JobStatus.PENDING.getValue();
            }
        } else if (statusCountMap.get(JobStatus.FAILED.getValue()) > 0 || statusCountMap.get(JobStatus.ABORTED.getValue()) > 0) {
            groupStatus = JobStatus.FAILED.getValue();
        } else if (statusCountMap.get(JobStatus.COMPLETED.getValue()) == groupJobs.size()) {
            groupStatus = JobStatus.COMPLETED.getValue();
        }

        //如果组状态不一致（防止重复调用），并且组状态是completed则判断是否fire下一组
        if (!groupStatus.equals(groupVo.getStatus())) {
            Long nextGroupId = deployBatchJobMapper.getNextGroupId(groupVo.getLaneId(), groupVo.getSort());
            logger.info("Batch run update group:#" + groupId + " status:" + groupStatus);
            groupVo.setStatus(groupStatus);
            deployBatchJobMapper.updateGroupStatus(groupVo);
            if (Objects.equals(groupStatus, JobStatus.COMPLETED.getValue())) {
                if (nextGroupId == null) {
                    fireLaneNextGroup(groupVo, nextGroupId);
                } else if (groupVo.getNeedWait() == 0) {
                    fireLaneNextGroup(groupVo, nextGroupId);
                }
            }
        }
    }

    /**
     * 激活该泳道下一组
     *
     * @param currentGroupVo 当前组
     * @param nextGroupId    下一组id
     */
    public void fireLaneNextGroup(LaneGroupVo currentGroupVo, Long nextGroupId) {
        if (nextGroupId != null) {
            logger.info("Next group found:#" + nextGroupId + ", for lane:#" + currentGroupVo.getLaneId() + " pre sort:#" + currentGroupVo.getSort());
            fireLaneGroup(nextGroupId);
        } else {
            deployBatchJobMapper.updateLaneStatus(currentGroupVo.getLaneId(), JobStatus.COMPLETED.getValue());
            logger.info("Batch run lane:#" + currentGroupVo.getLaneId() + " finished.");
            LaneVo laneVo = deployBatchJobMapper.getLaneById(currentGroupVo.getLaneId());
            Long batchJobId = laneVo.getBatchJobId();
            List<LaneVo> laneList = deployBatchJobMapper.getLaneListByBatchJobId(batchJobId);
            boolean isCompleted = true;
            for (LaneVo lane : laneList) {
                if (!Objects.equals(JobStatus.COMPLETED.getValue(), lane.getStatus())) {
                    isCompleted = false;
                    break;
                }
            }
            if (isCompleted) {
                AutoexecJobVo batchJobVo = new AutoexecJobVo();
                batchJobVo.setId(batchJobId);
                batchJobVo.setStatus(JobStatus.COMPLETED.getValue());
                autoexecJobMapper.updateJobStatus(batchJobVo);
            }
        }
    }


}
