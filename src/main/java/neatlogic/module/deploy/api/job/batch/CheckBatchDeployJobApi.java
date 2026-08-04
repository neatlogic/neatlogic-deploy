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

package neatlogic.module.deploy.api.job.batch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.exception.DeployBatchJobCannotCheckException;
import neatlogic.framework.deploy.exception.DeployBatchJobNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.auth.core.BatchDeployAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployBatchJobMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/8/10 15:20
 **/

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CheckBatchDeployJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public String getName() {
        return "nmdajb.checkbatchdeployjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "nmdajb.checkbatchdeployjobapi.input.param.desc.id", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "nmdajb.checkbatchdeployjobapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long batchJobId = jsonObj.getLong("id");
        DeployJobVo deployBatchJobVo = deployBatchJobMapper.getBatchDeployJobLockById(batchJobId);
        if (deployBatchJobVo == null) {
            throw new DeployBatchJobNotFoundException(batchJobId);
        }
        if(BatchDeployAuthChecker.isCanCheck(deployBatchJobVo)) {
            throw new DeployBatchJobCannotCheckException();
        }
        List<AutoexecJobVo> jobVoList = autoexecJobMapper.getJobListLockByParentIdAndStatus(batchJobId, JobStatus.COMPLETED.getValue());
        if (CollectionUtils.isNotEmpty(jobVoList)) {
            for (AutoexecJobVo jobVo : jobVoList) {
                jobVo.setAction(JobAction.CHECK.getValue());
                IAutoexecJobActionHandler action = AutoexecJobActionHandlerFactory.getAction(JobAction.CHECK.getValue());
                action.doService(jobVo);
            }
        }
        jobVoList = autoexecJobMapper.getJobListByParentIdAndNotInStatus(batchJobId, JobStatus.CHECKED.getValue());
        if (CollectionUtils.isEmpty(jobVoList)) {
            autoexecJobMapper.updateJobStatus(new AutoexecJobVo(batchJobId, JobStatus.CHECKED.getValue()));
        }

        return null;
    }

    @Override
    public String getToken() {
        return "deploy/batchjob/check";
    }
}
