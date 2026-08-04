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
import neatlogic.framework.deploy.constvalue.DeployWebhookBuildNoPolicy;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookAppModuleVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import neatlogic.framework.deploy.exception.webhook.DeployWebhookNameRepeatException;
import neatlogic.framework.deploy.exception.webhook.DeployWebhookNotFoundException;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployJobWebhookApi extends PrivateApiComponentBase {
    @Resource
    DeployJobWebhookMapper webhookMapper;

    @Override
    public String getName() {
        return "nmdaw.savedeployjobwebhookapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.name"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.isactive"),
            @Param(name = "integrationUuid", type = ApiParamType.STRING, isRequired = true, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.integrationuuid"),
            @Param(name = "type", type = ApiParamType.ENUM, member = ScheduleType.class, isRequired = true, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.type"),
            @Param(name = "pipelineType", type = ApiParamType.ENUM, member = PipelineType.class, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.pipelinetype"),
            @Param(name = "buildNoPolicy", type = ApiParamType.ENUM, member = DeployWebhookBuildNoPolicy.class, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.buildnopolicy"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "nmdaw.savedeployjobwebhookapi.input.param.desc.config")

    })
    @Output({})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployJobWebhookVo deployJobWebhookVo = paramObj.toJavaObject(DeployJobWebhookVo.class);
        if (webhookMapper.checkWebhookNameIsExist(deployJobWebhookVo.getId(), deployJobWebhookVo.getName()) > 0) {
            throw new DeployWebhookNameRepeatException(deployJobWebhookVo.getName());
        }
        if (id == null) {
            webhookMapper.insertJobWebhook(deployJobWebhookVo);
        } else {
            DeployJobWebhookVo oldWebhook = webhookMapper.getWebhookById(id);
            if (oldWebhook == null) {
                throw new DeployWebhookNotFoundException(id);
            }
            webhookMapper.deleteWebhookByIdAppModuleByWebhookId(id);
            webhookMapper.updateJobWebhook(deployJobWebhookVo);
        }
        if (Objects.equals(ScheduleType.GENERAL.getValue(), deployJobWebhookVo.getType())) {
            List<DeployJobWebhookAppModuleVo> appModuleVoList = deployJobWebhookVo.getConfig().getWebhookAppModuleList();
            if (CollectionUtils.isNotEmpty(appModuleVoList)) {
                for (DeployJobWebhookAppModuleVo appModuleVo : appModuleVoList) {
                    appModuleVo.setWebhookId(deployJobWebhookVo.getId());
                    webhookMapper.insertJobWebhookAppModule(appModuleVo);
                }
            }
        }
        return null;
    }

    public IValid name() {
        return value -> {
            DeployJobWebhookVo deployJobWebhookVo = JSONObject.toJavaObject(value, DeployJobWebhookVo.class);
            if (webhookMapper.checkWebhookNameIsExist(deployJobWebhookVo.getId(), deployJobWebhookVo.getName()) > 0) {
                return new FieldValidResultVo(new DeployWebhookNameRepeatException(deployJobWebhookVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/save";
    }
}
