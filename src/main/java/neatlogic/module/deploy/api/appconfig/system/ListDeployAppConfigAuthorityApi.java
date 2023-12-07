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

package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployAppConfigActionType;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/6/15 11:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAuthorityApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "/deploy/app/config/authority/action/list";
    }

    @Override
    public String getName() {
        return "nmdaas.listdeployappconfigauthorityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaas.listdeployappconfigauthorityapi.input.param.desc.appid"),
            @Param(name = "includeActionList", type = ApiParamType.JSONARRAY, desc = "nmdaas.searchdeployappconfigauthorityapi.input.param.desc.includeactionlist")
    })
    @Output({
    })
    @Description(desc = "nmdaas.listdeployappconfigauthorityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        JSONArray includeActionList = paramObj.getJSONArray("includeActionList");
        boolean isNeedScenario = org.apache.commons.collections4.CollectionUtils.isEmpty(includeActionList) || includeActionList.contains(DeployAppConfigActionType.SCENARIO.getValue());
        boolean isNeedEnv = org.apache.commons.collections4.CollectionUtils.isEmpty(includeActionList) || includeActionList.contains(DeployAppConfigActionType.ENV.getValue());
        JSONObject returnObj = new JSONObject();

        //操作权限
        returnObj.put("operationAuthList", DeployAppConfigAction.getValueTextList(includeActionList));

        //场景权限
        if (isNeedScenario) {
            DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId).getConfig();
            if (pipelineConfigVo == null) {
                throw new DeployAppConfigNotFoundException(appSystemId);
            }
            List<JSONObject> scenarioAuthList = new ArrayList<>();
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                JSONObject scenarioValueText = new JSONObject();
                scenarioValueText.put("text", scenarioVo.getScenarioName());
                scenarioValueText.put("value", scenarioVo.getScenarioId());
                scenarioAuthList.add(scenarioValueText);
            }
            returnObj.put("scenarioAuthList", scenarioAuthList);

        }
        //环境权限
        if (isNeedEnv) {
            List<JSONObject> envAuthList = new ArrayList<>();
            List<DeployAppEnvironmentVo> envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>());
            for (DeployAppEnvironmentVo env : envList) {
                JSONObject envValueText = new JSONObject();
                envValueText.put("text", env.getName());
                envValueText.put("value", env.getId());
                envAuthList.add(envValueText);
            }
            returnObj.put("envAuthList", envAuthList);
        }
        return returnObj;
    }
}
