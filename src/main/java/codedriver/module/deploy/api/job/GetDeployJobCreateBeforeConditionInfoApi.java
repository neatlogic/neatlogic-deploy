/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppSystemVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployJobCreateBeforeConditionInfoApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取创建发布作业之前的条件信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/job/create/before/condition/info/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "系统id")
    })
    @Output({
            @Param(name = "isConfig", type = ApiParamType.INTEGER, desc = "是否拥有环境"),
            @Param(name = "isHasEnv", type = ApiParamType.INTEGER, desc = "是否拥有环境"),
            @Param(name = "isHasModule", type = ApiParamType.INTEGER, desc = "是否拥有环境")
    })
    @Description(desc = "获取创建发布作业之前的条件信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        DeployAppSystemVo appSystemVo = deployAppConfigMapper.getDeployJobCreateBeforeConditionInfo(paramObj.getLong("appSystemId"));
        if (appSystemVo == null) {
            return null;
        }
        returnObj.put("isConfig", appSystemVo.getIsConfig());
        returnObj.put("isHasModule", appSystemVo.getIsHasModule());
        if (CollectionUtils.isNotEmpty(deployAppConfigMapper.getDeployAppSystemListIncludeEnvIdListByAppSystemIdList(Collections.singletonList(paramObj.getLong("appSystemId")), TenantContext.get().getDataDbName()))) {
            returnObj.put("isHasEnv", 1);
        }
        return returnObj;
    }
}
