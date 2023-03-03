/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.api.webhook;

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
import com.alibaba.fastjson.JSONObject;
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
        return "保存发布作业触发器";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "触发器id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "触发器名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "integrationUuid", type = ApiParamType.STRING, isRequired = true, desc = "集成uuid"),
            @Param(name = "type", type = ApiParamType.ENUM, member = ScheduleType.class, isRequired = true, desc = "作业类型"),
            @Param(name = "pipelineType", type = ApiParamType.ENUM, member = PipelineType.class, desc = "流水线类型"),
            @Param(name = "buildNoPolicy", type = ApiParamType.ENUM, member = DeployWebhookBuildNoPolicy.class, desc = "编译号策略"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")

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
