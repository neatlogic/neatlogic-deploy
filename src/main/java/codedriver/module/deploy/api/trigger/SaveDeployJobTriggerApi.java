/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.trigger;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.trigger.DeployJobTriggerVo;
import codedriver.framework.deploy.exception.trigger.DeployTriggerNameRepeatException;
import codedriver.framework.deploy.exception.trigger.DeployTriggerNotFoundException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobTriggerMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployJobTriggerApi extends PrivateApiComponentBase {
    @Resource
    DeployJobTriggerMapper triggerMapper;

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
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")

    })
    @Output({})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployJobTriggerVo deployJobTriggerVo = paramObj.toJavaObject(DeployJobTriggerVo.class);
        if(triggerMapper.checkTriggerNameIsExist( id, deployJobTriggerVo.getName()) > 0){
            throw new DeployTriggerNameRepeatException(deployJobTriggerVo.getName());
        }
        if(id == null){
            triggerMapper.insertJobTrigger(deployJobTriggerVo);
        }else{
            DeployJobTriggerVo oldTrigger = triggerMapper.getTriggerById(id);
            if(oldTrigger == null){
                throw new DeployTriggerNotFoundException(id);
            }
            triggerMapper.updateJobTrigger(deployJobTriggerVo);
        }
        return null;
    }

    public IValid name() {
        return value -> {
            DeployJobTriggerVo deployJobTriggerVo = JSONObject.toJavaObject(value, DeployJobTriggerVo.class);
            if(triggerMapper.checkTriggerNameIsExist(deployJobTriggerVo.getId(),deployJobTriggerVo.getName()) >0){
                return new FieldValidResultVo(new DeployTriggerNameRepeatException(deployJobTriggerVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

    @Override
    public String getToken() {
        return "/deploy/job/trigger/save";
    }
}
