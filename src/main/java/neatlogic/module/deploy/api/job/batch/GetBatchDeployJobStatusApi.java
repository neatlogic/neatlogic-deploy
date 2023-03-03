/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.job.batch;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = DEPLOY_BASE.class)
public class GetBatchDeployJobStatusApi extends PrivateApiComponentBase {

    @Resource
    DeployJobMapper deployJobMapper;

    @Override
    public String getName() {
        return "获取单个批量作业状态信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "作业id")})
    @Output({@Param(explode = DeployJobVo.class)})
    @Description(desc = "获取单个批量作业状态信息接口，主要用于刷新状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return deployJobMapper.getJobBaseInfoById(jsonObj.getLong("id"));
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/status/get";
    }
}
