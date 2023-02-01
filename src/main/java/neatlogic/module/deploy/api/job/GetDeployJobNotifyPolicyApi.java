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
package neatlogic.module.deploy.api.job;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/12/30 18:01
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployJobNotifyPolicyApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取发布应用的通知策略id";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id")
    })
    @Description(desc = "获取发布应用的通知策略id")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return deployAppConfigMapper.getAppSystemNotifyPolicyIdByAppSystemId(paramObj.getLong("appSystemId"));
    }

    @Override
    public String getToken() {
        return "deploy/job/notify/policy/get";
    }
}
