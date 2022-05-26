/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.app;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppModuleRunnerGroupSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/module/runner/group/save";
    }

    @Override
    public String getName() {
        return "保存应用模块runner组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "runnerGroupId", type = ApiParamType.LONG, isRequired = true, desc = "runner组id")
    })
    @Output({
    })
    @Description(desc = "保存应用模块runner组接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long moduleId = paramObj.getLong("moduleId");
        Long runnerGroupId = paramObj.getLong("runnerGroupId");
        deployAppConfigMapper.insertAppModuleRunnerGroup(moduleId,runnerGroupId);
       return null;
    }
}
