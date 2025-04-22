/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package neatlogic.module.deploy.api.bluegreen;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployInstanceBlueGreenVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployBlueGreenMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/12/8 14:42
 */

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveInstanceBlueGreenApi extends PrivateApiComponentBase {

    @Resource
    DeployBlueGreenMapper deployBlueGreenMapper;

    @Override
    public String getName() {
        return "nmdab.saveinstancebluegreenapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/instance/bluegreen/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id", isRequired = true),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id", isRequired = true),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "实例id", isRequired = true),
            @Param(name = "blueGreenId", type = ApiParamType.LONG, desc = "蓝绿id")
    })
    @Description(desc = "nmdab.saveinstancebluegreenapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployInstanceBlueGreenVo instanceBlueGreenVo = JSON.toJavaObject(paramObj, DeployInstanceBlueGreenVo.class);
        Long blueGreenId = paramObj.getLong("blueGreenId");
        if (blueGreenId != null) {
            return deployBlueGreenMapper.insertInstanceBlueGreen(instanceBlueGreenVo);
        } else {
            return deployBlueGreenMapper.deleteInstanceBlueGreen(instanceBlueGreenVo);
        }
    }
}
