/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.auth.core.BatchDeployAuthChecker;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        DeployJobVo deployJobVo = deployJobMapper.getBatchDeployJobById(jsonObj.getLong("id"), TenantContext.get().getDataDbName());
        deployJobVo.setIsCanExecute(BatchDeployAuthChecker.isCanExecute(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanTakeOver(BatchDeployAuthChecker.isCanTakeOver(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanEdit(BatchDeployAuthChecker.isCanEdit(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanCheck(BatchDeployAuthChecker.isCanCheck(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanGroupExecute(BatchDeployAuthChecker.isCanGroupExecute(deployJobVo) ? 1 : 0);
        return deployJobVo;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/get";
    }
}
