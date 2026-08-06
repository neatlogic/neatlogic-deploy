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
package neatlogic.module.deploy.api.webhook;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import neatlogic.framework.deploy.exception.webhook.DeployWebhookNotFoundException;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/9/27 10:40
 */

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ActivateDeployJobWebhookApi extends PrivateApiComponentBase {

    @Resource
    DeployJobWebhookMapper webhookMapper;

    @Override
    public String getName() {
        return "nmdaw.activatedeployjobwebhookapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/activate";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmdaw.activatedeployjobwebhookapi.input.param.desc.id"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "nmdaw.activatedeployjobwebhookapi.input.param.desc.isactive")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployJobWebhookVo oldWebhook = webhookMapper.getWebhookById(id);
        if (oldWebhook == null) {
            throw new DeployWebhookNotFoundException(id);
        }
        webhookMapper.ActivateJobWebhookById(id, paramObj.getInteger("isActive"));
        return null;
    }
}
