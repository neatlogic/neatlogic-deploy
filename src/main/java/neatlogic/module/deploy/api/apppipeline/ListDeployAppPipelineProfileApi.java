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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployProfileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineProfileApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmdaa.listdeployapppipelineprofileapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/List";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaa.listdeployapppipelineprofileapi.input.param.desc.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "nmdaa.listdeployapppipelineprofileapi.input.param.desc.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "nmdaa.listdeployapppipelineprofileapi.input.param.desc.envid"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "nmdaa.listdeployapppipelineprofileapi.input.param.desc.defaultvalue")
    })
    @Output({
            @Param(name = "Return", explode = DeployProfileVo[].class, desc = "nmdaa.listdeployapppipelineprofileapi.output.param.desc.return")
    })
    @Description(desc = "nmdaa.listdeployapppipelineprofileapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(searchVo.getAppSystemId());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(searchVo.getAppSystemId());
        }
        searchVo.setAppSystemName(appSystem.getName());
        searchVo.setAppSystemAbbrName(appSystem.getAbbrName());
        Long appModuleId = searchVo.getAppModuleId();
        if (appModuleId != null && appModuleId != 0) {
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
            if (appModule == null) {
                throw new AppModuleNotFoundException(appModuleId);
            }
            searchVo.setAppModuleName(appModule.getName());
            searchVo.setAppModuleAbbrName(appModule.getAbbrName());
        }
        Long envId = searchVo.getEnvId();
        if (envId != null && envId != 0) {
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId);
            if (env == null) {
                throw new AppEnvNotFoundException(envId);
            }
            searchVo.setEnvName(env.getName());
        }
        List<Long> profileIdList = new ArrayList<>();
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            profileIdList = defaultValue.toJavaList(Long.class);
        }
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(searchVo.getAppSystemId())
                .withAppModuleId(searchVo.getAppModuleId())
                .withEnvId(searchVo.getEnvId())
                .withProfileIdList(profileIdList)
                .withIsDeleteDisabledPhase(false)
                .withIsHasBuildOrDeployTypeTool(false)
                .withIsUpdateConfig(false)
                .withIsUpdateProfile(true)
                .getConfig();
        return deployPipelineConfigVo.getOverrideProfileList();
    }
}
