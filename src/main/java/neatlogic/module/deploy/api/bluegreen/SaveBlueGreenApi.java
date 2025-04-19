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
import neatlogic.framework.deploy.dto.app.DeployBlueGreenVo;
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
public class SaveBlueGreenApi extends PrivateApiComponentBase {

    @Resource
    DeployBlueGreenMapper deployBlueGreenMapper;

    @Override
    public String getName() {
        return "nmdab.savebluegreenapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/bluegreen/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name", isRequired = true, xss = true),
            @Param(name = "sort", type = ApiParamType.INTEGER, desc = "common.sort", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive", isRequired = true),
    })
    @Description(desc = "nmdab.savebluegreenapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployBlueGreenVo blueGreenVo = JSON.toJavaObject(paramObj, DeployBlueGreenVo.class);
        if(id != null){
            deployBlueGreenMapper.updateBlueGreen(blueGreenVo);
        }else{
            deployBlueGreenMapper.insertBlueGreen(blueGreenVo);
        }
        return null;
    }
}
