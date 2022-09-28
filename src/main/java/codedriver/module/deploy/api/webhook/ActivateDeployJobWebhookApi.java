/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.webhook;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookVo;
import codedriver.framework.deploy.exception.webhook.DeployWebhookNotFoundException;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobWebhookMapper;
import com.alibaba.fastjson.JSONObject;
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
        return "激活/禁用触发器配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/activate";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "触发器id"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活（1：激活，0：禁用）")
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
