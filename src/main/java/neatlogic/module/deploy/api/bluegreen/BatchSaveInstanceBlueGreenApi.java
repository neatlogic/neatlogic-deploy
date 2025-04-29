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

import com.alibaba.fastjson.JSONArray;
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
import java.util.HashSet;
import java.util.Set;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class BatchSaveInstanceBlueGreenApi extends PrivateApiComponentBase {

    @Resource
    DeployBlueGreenMapper deployBlueGreenMapper;

    @Override
    public String getName() {
        return "nmdab.batchsaveinstancebluegreenapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/instance/bluegreen/batchsave";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id", isRequired = true),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id", isRequired = true),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id", isRequired = true),
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, desc = "实例id列表", minSize = 1, isRequired = true),
            @Param(name = "blueGreenId", type = ApiParamType.LONG, isRequired = true, desc = "蓝绿id")
    })
    @Description(desc = "nmdab.batchsaveinstancebluegreenapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        Long blueGreenId = paramObj.getLong("blueGreenId");
        DeployInstanceBlueGreenVo instanceBlueGreenVo = new DeployInstanceBlueGreenVo();
        instanceBlueGreenVo.setAppSystemId(appSystemId);
        instanceBlueGreenVo.setAppModuleId(appModuleId);
        instanceBlueGreenVo.setEnvId(envId);
        instanceBlueGreenVo.setBlueGreenId(blueGreenId);
        Set<Long> resourceIdSet = new HashSet<>();
        JSONArray resourceIdList = paramObj.getJSONArray("resourceIdList");
        for (int i = 0; i < resourceIdList.size(); i++) {
            Long resourceId = resourceIdList.getLong(i);
            if (resourceId != null && !resourceIdSet.contains(resourceId)) {
                instanceBlueGreenVo.setResourceId(resourceId);
                deployBlueGreenMapper.insertInstanceBlueGreen(instanceBlueGreenVo);
                resourceIdSet.add(resourceId);
            }
        }
        return null;
    }
}
