/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
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

    @Override
    public String getName() {
        return "验证批量发布作业";
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
    @Description(desc = "验证批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long batchJobId = jsonObj.getLong("id");
        List<AutoexecJobVo> jobVoList = autoexecJobMapper.getJobListLockByParentIdAndStatus(batchJobId, JobStatus.COMPLETED.getValue());
        if(CollectionUtils.isNotEmpty(jobVoList)){
            for (AutoexecJobVo jobVo : jobVoList){
                jobVo.setAction(JobAction.CHECK.getValue());
                IAutoexecJobActionHandler action = AutoexecJobActionHandlerFactory.getAction(JobAction.CHECK.getValue());
                action.doService(jobVo);
            }
        }
        jobVoList = autoexecJobMapper.getJobListByParentIdAndNotInStatus(batchJobId, JobStatus.CHECKED.getValue());
        if(CollectionUtils.isEmpty(jobVoList)){
            autoexecJobMapper.updateJobStatus(new AutoexecJobVo(batchJobId,JobStatus.CHECKED.getValue()));
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/batchjob/check";
    }
}
