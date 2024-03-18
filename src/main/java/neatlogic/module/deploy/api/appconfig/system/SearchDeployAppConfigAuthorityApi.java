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
        return "nmdaas.searchdeployappconfigauthorityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaas.listdeployappconfigauthorityapi.input.param.desc.appid"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, desc = "common.userlist"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, desc = "nmdaas.searchdeployappconfigauthorityapi.input.param.desc.actionlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "common.needpage"),
            @Param(name = "includeActionList", type = ApiParamType.JSONARRAY, desc = "nmdaas.searchdeployappconfigauthorityapi.input.param.desc.includeactionlist")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigAuthorityVo[].class, desc = "应用配置授权列表")
    })
    @Description(desc = "查询应用配置权限接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray includeActionList = paramObj.getJSONArray("includeActionList");
        paramObj.remove("includeActionList");
        DeployAppConfigAuthorityVo searchVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);
        List<JSONObject> theadList = new ArrayList<>();
        boolean isNeedScenario = CollectionUtils.isEmpty(includeActionList) || includeActionList.contains(DeployAppConfigActionType.SCENARIO.getValue());
        boolean isNeedEnv = CollectionUtils.isEmpty(includeActionList) || includeActionList.contains(DeployAppConfigActionType.ENV.getValue());
        theadList.add(new JSONObject() {{
            put("name", "user");
            put("displayName", $.t("用户"));
        }});
        for (DeployAppConfigAction action : DeployAppConfigAction.values()) {
            if (CollectionUtils.isEmpty(includeActionList) || includeActionList.contains(action.getValue())) {
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
