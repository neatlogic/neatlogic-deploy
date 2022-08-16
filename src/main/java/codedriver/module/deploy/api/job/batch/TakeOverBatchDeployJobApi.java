/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.exception.DeployBatchJobCannotTakeOverException;
import codedriver.framework.deploy.exception.DeployBatchJobNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.auth.core.BatchDeployAuthChecker;
import codedriver.module.deploy.dao.mapper.DeployBatchJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/8/12 15:20
 **/

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TakeOverBatchDeployJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public String getName() {
        return "接管批量发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "批量发布作业id", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "接管批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long batchJobId = jsonObj.getLong("id");
        DeployJobVo deployBatchJobVo = deployBatchJobMapper.getBatchDeployJobLockById(batchJobId);
        if (deployBatchJobVo == null) {
            throw new DeployBatchJobNotFoundException(batchJobId);
        }
        if (BatchDeployAuthChecker.isCanTakeOver(deployBatchJobVo)) {
            deployBatchJobVo.setExecUser(UserContext.get().getUserUuid(true));
            autoexecJobMapper.updateJobExecUser(deployBatchJobVo.getId(), deployBatchJobVo.getExecUser());
        } else {
            throw new DeployBatchJobCannotTakeOverException();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/batchjob/takeover";
    }
}
