/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundEditTargetException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundEditTargetException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppPipelineApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmdaa.getdeployapppipelineapi.getname";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "isDeleteDisabledPhase", type = ApiParamType.BOOLEAN, defaultValue = "false", desc = "nmdaa.getdeployapppipelineapi.input.param.desc.isdeletedisabledphase")
    })
    @Output({
            @Param(name = "Return", explode = DeployAppConfigVo.class, desc = "term.deploy.apppipelineinfo")
    })
    @Description(desc = "nmdaa.getdeployapppipelineapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(searchVo.getAppSystemId());
        if (appSystem == null) {
            throw new AppSystemNotFoundEditTargetException(searchVo.getAppSystemId());
        }
        searchVo.setAppSystemName(appSystem.getName());
        searchVo.setAppSystemAbbrName(appSystem.getAbbrName());
        Long appModuleId = searchVo.getAppModuleId();
        if (appModuleId != null && appModuleId != 0) {
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
            if (appModule == null) {
                throw new AppModuleNotFoundEditTargetException(appModuleId);
            }
            searchVo.setAppModuleName(appModule.getName());
            searchVo.setAppModuleAbbrName(appModule.getAbbrName());
        }
        Long envId = searchVo.getEnvId();
        if (envId != null && envId != 0) {
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId);
            if (env == null) {
                throw new AppEnvNotFoundEditTargetException(envId);
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
