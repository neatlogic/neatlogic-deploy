/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
    List<JSONObject> theadList = new ArrayList<>();
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppPipelineService deployAppPipelineService;

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

    //拼装默认theadList
    {
        theadList.add(new JSONObject() {{
            put("name", "user");
            put("displayName", "用户");
        }});
        theadList.add(new JSONObject() {{
            put("name", "envName");
            put("displayName", "环境");
        }});
        for (DeployAppConfigAction action : DeployAppConfigAction.values()) {
            JSONObject thead = new JSONObject();
            thead.put("name", action.getValue());
            thead.put("displayName", action.getText());
            theadList.add(thead);
        }
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境Id列表"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, desc = "用户列表"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, desc = "动作列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigAuthorityVo[].class, desc = "应用配置授权列表")
    })
    @Description(desc = "查询应用配置权限接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployAppConfigAuthorityVo searchVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);

        //根据appSystemId获取对应的场景theadList
        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(paramObj.getLong("appSystemId")));
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(paramObj.getLong("appSystemId"));
        }

        JSONArray finalTheadList = JSONArray.parseArray(theadList.toString());
        if(CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                JSONObject scenarioKeyValue = new JSONObject();
                scenarioKeyValue.put("name", scenarioVo.getScenarioName());
                scenarioKeyValue.put("displayName", scenarioVo.getScenarioName());
                finalTheadList.add(scenarioKeyValue);
            }
        }

        //获取tbodyList
        List<JSONObject> bodyList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppConfigAuthorityCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<DeployAppConfigAuthorityVo> appConfigAuthList = deployAppConfigMapper.getAppConfigAuthorityList(searchVo);
            if (CollectionUtils.isNotEmpty(appConfigAuthList)) {
                //获取当前应用下的所有环境
                List<DeployAppEnvironmentVo> envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), new ArrayList<>(), TenantContext.get().getDataDbName());
                Map<Long, String> envIdNameMap = new HashMap<>();
                List<String> scenarioList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(envList)) {
                    envIdNameMap = envList.stream().collect(Collectors.toMap(DeployAppEnvironmentVo::getId, DeployAppEnvironmentVo::getName));
                }
                if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
                    scenarioList = pipelineConfigVo.getScenarioList().stream().map(AutoexecCombopScenarioVo::getScenarioName).collect(Collectors.toList());
                }

                for (DeployAppConfigAuthorityVo appConfigAuthorityVo : appConfigAuthList) {
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
                            } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue())) {
                                if (StringUtils.equals(actionVo.getAction(), "all")) {
                                    for (DeployAppEnvironmentVo env : envList) {
                                        actionAuth.put(env.getName(), 1);
                                    }
                                } else {
                                    String envName = envIdNameMap.get(Long.valueOf(actionVo.getAction()));
                                    if (StringUtils.isNotEmpty(envName)) {
                                        actionAuth.put(envName, 1);
                                    }
                                }

                                //场景权限
                            } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue())) {
                                if (StringUtils.equals(actionVo.getAction(), "all")) {
                                    for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                                        actionAuth.put(scenarioVo.getScenarioName(), 1);
                                    }
                                } else {
                                    if (scenarioList.contains(actionVo.getAction())) {
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
