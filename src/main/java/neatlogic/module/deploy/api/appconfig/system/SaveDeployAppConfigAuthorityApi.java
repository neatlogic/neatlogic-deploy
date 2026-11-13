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

package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployApplicationWriter;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAuthorityApi extends PrivateApiComponentBase {

    @Resource
    private DeployApplicationWriter writer;
    @Override
    public String getToken() {
        return "deploy/app/config/authority/save";
    }

    @Override
    public String getName() {
        return "nmdaas.savedeployappconfigauthorityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "common.authlist"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "nmdaas.savedeployappconfigauthorityapi.input.param.desc.actionlist"),
            @Param(name = "isEdit", type = ApiParamType.INTEGER, isRequired = true, desc = "nmdaas.savedeployappconfigauthorityapi.input.param.desc.isedit"),
    })
    @Output({
    })
    @Description(desc = "nmdaas.savedeployappconfigauthorityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return writer.saveAuthority(paramObj);
    }
}
