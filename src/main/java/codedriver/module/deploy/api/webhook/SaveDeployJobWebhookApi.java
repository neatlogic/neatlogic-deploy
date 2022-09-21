/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.webhook;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployWebhookBuildNoPolicy;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookAppModuleVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookVo;
import codedriver.framework.deploy.exception.webhook.DeployWebhookNameRepeatException;
import codedriver.framework.deploy.exception.webhook.DeployWebhookNotFoundException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobWebhookMapper;
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
        if(webhookMapper.checkWebhookNameIsExist( deployJobWebhookVo.getId(), deployJobWebhookVo.getName()) > 0){
            throw new DeployWebhookNameRepeatException(deployJobWebhookVo.getName());
        }
        if(id == null){
            webhookMapper.insertJobWebhook(deployJobWebhookVo);
            if(Objects.equals(ScheduleType.GENERAL.getValue(),deployJobWebhookVo.getType())){
                List<DeployJobWebhookAppModuleVo> appModuleVoList = deployJobWebhookVo.getConfig().getWebhookAppModuleList();
                if(CollectionUtils.isNotEmpty(appModuleVoList)){
                    for(DeployJobWebhookAppModuleVo appModuleVo : appModuleVoList) {
                        appModuleVo.setWebhookId(deployJobWebhookVo.getId());
                        webhookMapper.insertJobWebhookAppModule(appModuleVo);
                    }
                }
            }
        }else{
            DeployJobWebhookVo oldWebhook = webhookMapper.getWebhookById(id);
            if(oldWebhook == null){
                throw new DeployWebhookNotFoundException(id);
            }
            webhookMapper.updateJobWebhook(deployJobWebhookVo);
        }
        return null;
    }

    public IValid name() {
        return value -> {
            DeployJobWebhookVo deployJobWebhookVo = JSONObject.toJavaObject(value, DeployJobWebhookVo.class);
            if(webhookMapper.checkWebhookNameIsExist(deployJobWebhookVo.getId(),deployJobWebhookVo.getName()) >0){
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
