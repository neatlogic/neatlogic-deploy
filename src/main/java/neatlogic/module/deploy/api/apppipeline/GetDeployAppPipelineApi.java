/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.apppipeline;

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
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppPipelineApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "isDeleteDisabledPhase", type = ApiParamType.BOOLEAN, defaultValue = "false", desc = "是否删除禁用阶段")
    })
    @Output({
            @Param(name = "Return", explode = DeployAppConfigVo.class, desc = "应用流水线配置信息")
    })
    @Description(desc = "获取应用流水线")
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
        boolean isDeleteDisabledPhase = paramObj.getBooleanValue("isDeleteDisabledPhase");
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(searchVo.getAppSystemId())
                .withAppModuleId(searchVo.getAppModuleId())
                .withEnvId(searchVo.getEnvId())
                .withDeleteDisabledPhase(isDeleteDisabledPhase) // 删除禁用阶段
                .getConfig();
        searchVo.setConfig(deployPipelineConfigVo);
        return searchVo;
    }
}
