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
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployBatchJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/8/5 11:20
 **/

@Transactional
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ExecuteBatchDeployJobGroupApi extends PrivateApiComponentBase {
    @Resource
    private DeployBatchJobService deployBatchJobService;

    @Override
    public String getName() {
        return "执行批量发布作业组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "批量作业组id"),
            @Param(name = "isGoon", type = ApiParamType.ENUM, rule = "1,0", isRequired = true, desc = "执行完当前组是否继续执行后续组，但仍受needWait约束，1：是，0：否"),
            @Param(name = "batchJobAction", type = ApiParamType.ENUM, rule = "refireAll,refireResetAll", isRequired = true, desc = "批量作业执行策略，refireAll：跳过所有已完成的子作业；refireResetAll:执行所有子作业"),
            @Param(name = "jobAction", type = ApiParamType.ENUM, rule = "refireAll,refireResetAll", isRequired = true, desc = "子作业执行策略，refireAll：跳过所有已完成、已忽略的节点；refireResetAll:执行所有节点"),
    })
    @Description(desc = "执行批量发布作业组接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        deployBatchJobService.refireLaneGroup(jsonObj.getLong("id"), jsonObj.getInteger("isGoon"), jsonObj.getString("batchJobAction"), jsonObj.getString("jobAction"));
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/execute/group";
    }

}
