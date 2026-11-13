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

package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployApplicationWriter;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.APP_CONFIG_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 保存应用系统基础信息，编辑已有应用时同时校验应用级编辑权限。
 *
 * @author longrf
 * @since  2022/7/14 4:17 下午
 */
@Service
@Transactional
@AuthAction(action = APP_CONFIG_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployApplicationWriter writer;
    @Override
    public String getName() {
        return "nmdaas.savedeployappconfigappsystemapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "abbrName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.sysname"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name"),
            @Param(name = "state", type = ApiParamType.JSONARRAY, desc = "common.status"),
            @Param(name = "owner", type = ApiParamType.JSONARRAY, desc = "common.maintenanceman"),
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "term.cmdb.maintenancewindow"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description")
    })
    @Output({@Param(name = "Return", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid")})
    @Description(desc = "nmdaas.savedeployappconfigappsystemapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return writer.saveSystem(paramObj, true);
    }
}
