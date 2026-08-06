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
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid", isRequired = true),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid", isRequired = true),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid", isRequired = true),
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, desc = "nmdab.batchsaveinstancebluegreenapi.input.param.desc.resourceidlist", minSize = 1, isRequired = true),
            @Param(name = "blueGreenId", type = ApiParamType.LONG, isRequired = true, desc = "nmdab.batchsaveinstancebluegreenapi.input.param.desc.bluegreenid")
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
