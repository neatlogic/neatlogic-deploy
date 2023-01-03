/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dependency.handler.NotifyPolicyDeployJobDependencyHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * @author longrf
 * @date 2022/12/29 17:11
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployJobNotifyPolicyApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "保存发布作业通知策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id")
    })
    @Output({
    })
    @Description(desc = "保存发布作业通知策略")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DependencyManager.delete(NotifyPolicyDeployJobDependencyHandler.class, paramObj.getLong("appSystemId"));
        if (Objects.nonNull(paramObj.getLong("notifyPolicyId"))) {
            DependencyManager.insert(NotifyPolicyDeployJobDependencyHandler.class, paramObj.getLong("notifyPolicyId"), paramObj.getLong("appSystemId"));
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/job/notify/policy/save";
    }
}
