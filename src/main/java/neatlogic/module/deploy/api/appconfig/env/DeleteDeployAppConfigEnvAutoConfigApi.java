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

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeleteDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/delete";
    }

    @Override
    public String getName() {
        return "nmdaae.deletedeployappconfigenvautoconfigapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvautoconfigapi.input.param.desc.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvautoconfigapi.input.param.desc.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvautoconfigapi.input.param.desc.envid"),
            @Param(name = "instanceId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvautoconfigapi.input.param.desc.instanceid"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.deletedeployappconfigenvautoconfigapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSONObject.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
        return null;
    }
}
