/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.BATCHDEPLOY_VERIFY;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = BATCHDEPLOY_VERIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class UpdateBatchDeployJobReviewStatusApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;


    @Override
    public String getName() {
        return "修改批量发布作业审核状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/updatereviewstatus";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "reviewStatus", type = ApiParamType.ENUM, member = ReviewStatus.class, isRequired = true, desc = "审批动作")})
    @Output({@Param(explode = DeployJobVo.class)})
    @Description(desc = "修改批量发布作业审核状态接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        deployJobVo.setReviewer(UserContext.get().getUserUuid(true));
        deployJobMapper.updateDeployJobReviewStatusById(deployJobVo);
        return deployJobMapper.getBatchDeployJobById(deployJobVo.getId());
    }

}
