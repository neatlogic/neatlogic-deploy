/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.AutoexecJobSourceVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobSourceInvalidException;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.exception.DeployBatchJobFireWithInvalidStatusException;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class DeployBatchJobServiceImpl implements DeployBatchJobService {
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public void fireBatch(DeployJobVo deployJobVo) {
        Long batchId = deployJobVo.getId();
        //TODO 执行权限判断
        deployJobMapper.getBatchDeployJobLockById(batchId);
        DeployJobVo batchJob = deployJobMapper.getDeployJobByJobId(batchId);
        //不允许存在"已撤销"的作业
        List<AutoexecJobVo> autoexecJobList = deployJobMapper.getBatchDeployJobListByIdAndWithoutStatus(batchId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        if (CollectionUtils.isNotEmpty(autoexecJobList)) {
            throw new DeployBatchJobFireWithInvalidStatusException(autoexecJobList);
        }

        //更新批量发布父作业状态和执行用户
        String loginUserUuid = UserContext.get().getUserUuid();
        if (!Objects.equals(loginUserUuid, deployJobVo.getExecUser())) {
            //TODO 批量执行权限 更换批量发布执行用户
        }
        deployJobVo.setStatus(JobStatus.PENDING.getValue());
        autoexecJobMapper.updateJobStatus(deployJobVo);

        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(deployJobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(deployJobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        //先判断是否有发布执行权限，如果有则接管
        autoexecJobSourceActionHandler.executeAuthCheck(deployJobVo.getId(), deployJobVo.getOperationId(), deployJobVo.getOperationType(), true, UserContext.get().getUserUuid());


    }
}
