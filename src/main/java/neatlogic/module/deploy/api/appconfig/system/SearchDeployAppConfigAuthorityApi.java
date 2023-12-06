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
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployAppConfigActionType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityActionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigAuthorityApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/authority/search";
    }

    @Override
    public String getName() {
        return "查询应用配置权限";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, desc = "用户列表"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, desc = "动作列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, desc = "需要的返回的权限action，如：['view','edit','scenario','env']")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigAuthorityVo[].class, desc = "应用配置授权列表")
    })
    @Description(desc = "查询应用配置权限接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray actionArray = paramObj.getJSONArray("actionList");
        paramObj.remove("actionList");
        DeployAppConfigAuthorityVo searchVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);
        List<JSONObject> theadList = new ArrayList<>();
        boolean isNeedScenario = CollectionUtils.isEmpty(actionArray) || actionArray.contains(DeployAppConfigActionType.SCENARIO.getValue());
        boolean isNeedEnv = CollectionUtils.isEmpty(actionArray) || actionArray.contains(DeployAppConfigActionType.ENV.getValue());
        theadList.add(new JSONObject() {{
            put("name", "user");
            put("displayName", $.t("用户"));
        }});
        for (DeployAppConfigAction action : DeployAppConfigAction.values()) {
            if (CollectionUtils.isEmpty(actionArray) || actionArray.contains(action.getValue())) {
                JSONObject thead = new JSONObject();
                thead.put("name", action.getValue());
                thead.put("displayName", action.getText());
                theadList.add(thead);
            }
        }
        JSONArray finalTheadList = JSONArray.parseArray(theadList.toString());

        //codehub 无需返回环境和场景thead
        List<DeployAppEnvironmentVo> envList = new ArrayList<>();
        DeployPipelineConfigVo pipelineConfigVo = new DeployPipelineConfigVo();
        Map<Long, String> envIdNameMap = new HashMap<>();
        if (isNeedEnv) {
            //获取当前应用下的所有环境
            envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), new ArrayList<>());
            for (DeployAppEnvironmentVo environmentVo : envList) {
                JSONObject envKeyValue = new JSONObject();
                envKeyValue.put("name", environmentVo.getId());
                envKeyValue.put("displayName", environmentVo.getName());
                finalTheadList.add(envKeyValue);
            }

            if (CollectionUtils.isNotEmpty(envList)) {
                envIdNameMap = envList.stream().collect(Collectors.toMap(DeployAppEnvironmentVo::getId, DeployAppEnvironmentVo::getName));
            }
        }
        List<Long> scenarioIdList = new ArrayList<>();
        if (isNeedScenario) {
            //根据appSystemId获取对应的场景theadList
            pipelineConfigVo = DeployPipelineConfigManager.init(paramObj.getLong("appSystemId")).getConfig();
            if (pipelineConfigVo == null) {
                throw new DeployAppConfigNotFoundException(paramObj.getLong("appSystemId"));
            }

            if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
                for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                    JSONObject scenarioKeyValue = new JSONObject();
                    scenarioKeyValue.put("name", scenarioVo.getScenarioId());
                    scenarioKeyValue.put("displayName", scenarioVo.getScenarioName());
                    finalTheadList.add(scenarioKeyValue);
                }
            }
            if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
                scenarioIdList = pipelineConfigVo.getScenarioList().stream().map(AutoexecCombopScenarioVo::getScenarioId).collect(Collectors.toList());
            }
        }

        //获取tbodyList
        List<JSONObject> bodyList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppConfigAuthorityCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<DeployAppConfigAuthorityVo> appConfigAuthList = deployAppConfigMapper.getAppConfigAuthorityList(searchVo);
            List<DeployAppConfigAuthorityVo> returnList = deployAppConfigMapper.getAppConfigAuthorityDetailList(appConfigAuthList);
            if (CollectionUtils.isNotEmpty(returnList)) {
                for (DeployAppConfigAuthorityVo appConfigAuthorityVo : returnList) {
                    List<DeployAppConfigAuthorityActionVo> actionList = appConfigAuthorityVo.getActionList();
                    JSONObject actionAuth = new JSONObject();
                    actionAuth.put("authUuid", appConfigAuthorityVo.getAuthUuid());
                    actionAuth.put("authType", appConfigAuthorityVo.getAuthType());

                    if (CollectionUtils.isNotEmpty(actionList)) {
                        for (DeployAppConfigAuthorityActionVo actionVo : actionList) {

                            //操作权限
                            if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.OPERATION.getValue())) {
                                if (StringUtils.equals(actionVo.getAction(), "all")) {
                                    for (JSONObject operation : DeployAppConfigAction.getValueTextList()) {
                                        actionAuth.put(operation.getString("value"), 1);
                                    }
                                } else {
                                    if (DeployAppConfigAction.getValueList().contains(actionVo.getAction())) {
                                        actionAuth.put(actionVo.getAction(), 1);
                                    }
                                }

                                //环境权限
                            } else if (isNeedEnv && StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue())) {
                                if (StringUtils.equals(actionVo.getAction(), "all")) {
                                    for (DeployAppEnvironmentVo env : envList) {
                                        actionAuth.put(env.getId().toString(), 1);
                                    }
                                } else {
                                    String envName = envIdNameMap.get(Long.valueOf(actionVo.getAction()));
                                    if (StringUtils.isNotEmpty(envName)) {
                                        actionAuth.put(actionVo.getAction(), 1);
                                    }
                                }

                                //场景权限
                            } else if (isNeedScenario && StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue())) {
                                if (StringUtils.equals(actionVo.getAction(), "all")) {
                                    for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                                        actionAuth.put(scenarioVo.getScenarioId().toString(), 1);
                                    }
                                } else {
                                    if (scenarioIdList.contains(Long.valueOf(actionVo.getAction()))) {
                                        actionAuth.put(actionVo.getAction(), 1);
                                    }
                                }
                            }
                        }
                        bodyList.add(actionAuth);
                    }
                }
            }
        }
        return TableResultUtil.getResult(finalTheadList, bodyList, searchVo);
    }
}
