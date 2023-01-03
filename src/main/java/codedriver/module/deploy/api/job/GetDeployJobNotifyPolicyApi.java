/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
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
