/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployEnvironmentWriter;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployEnvironmentWriter writer;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/save";
    }

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvautoconfigapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "deleteInstanceId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigenvautoconfigapi.input.param.desc.deleteinstanceid"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigenvautoconfigapi.input.param.desc.instanceid"),
            @Param(name = "keyValueList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigenvautoconfigapi.input.param.desc.keyvaluelist"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.savedeployappconfigenvautoconfigapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return writer.saveVariables(paramObj);
    }

}
