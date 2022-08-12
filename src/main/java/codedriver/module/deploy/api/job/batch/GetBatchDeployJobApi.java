/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = DEPLOY_BASE.class)
public class GetBatchDeployJobApi extends PrivateApiComponentBase {

    @Resource
    DeployJobMapper deployJobMapper;

    @Override
    public String getName() {
        return "获取单个批量作业信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "作业id")})
    @Output({@Param(explode = DeployJobVo.class)})
    @Description(desc = "获取单个批量作业信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployJobVo deployJobVo = deployJobMapper.getBatchDeployJobById(jsonObj.getLong("id"));
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())) {
            if (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
                if (!Objects.equals(JobStatus.RUNNING.getValue(), deployJobVo.getStatus())) {
                    if (UserContext.get().getUserUuid().equals(deployJobVo.getExecUser())) {
                        deployJobVo.setIsCanExecute(1);
                    }
                }
                int authCount = deployJobMapper.getDeployJobAuthCountByJobIdAndUuid(deployJobVo.getId(), UserContext.get().getUserUuid(true));
                if ((authCount > 0 || AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), DEPLOY_MODIFY.class.getSimpleName())) && !Objects.equals(deployJobVo.getExecUser(), UserContext.get().getUserUuid(true))) {
                    deployJobVo.setIsCanTakeOver(1);
                }
            }
            if (!Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.WAITING.getValue())) {
                if (Arrays.asList(JobStatus.PENDING.getValue(), JobStatus.SAVED.getValue(), JobStatus.COMPLETED.getValue(), JobStatus.FAILED.getValue()).contains(deployJobVo.getStatus())
                        && (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), DEPLOY_MODIFY.class.getSimpleName()) || Objects.equals(deployJobVo.getExecUser(), UserContext.get().getUserUuid(true)))) {
                    deployJobVo.setIsCanEdit(1);
                }
            }
        }

        return deployJobVo;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/get";
    }
}
