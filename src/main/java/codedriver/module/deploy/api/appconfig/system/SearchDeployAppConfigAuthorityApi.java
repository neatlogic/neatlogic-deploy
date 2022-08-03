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
    List<JSONObject> operationTheadList = new ArrayList<>();
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
        //用户
        theadList.add(new JSONObject() {{
            put("name", "user");
            put("displayName", "用户");
        }});
        //操作权限
        for (DeployAppConfigAction action : DeployAppConfigAction.values()) {
            JSONObject thead = new JSONObject();
            thead.put("name", action.getValue());
            thead.put("displayName", action.getText());
            operationTheadList.add(thead);
        }

        theadList.add(new JSONObject() {{
            put("list", operationTheadList);
            put("displayName", "操作权限");
        }});
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
        //获取当前应用下的所有环境
        List<DeployAppEnvironmentVo> envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), new ArrayList<>(), TenantContext.get().getDataDbName());

        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(paramObj.getLong("appSystemId")));
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(paramObj.getLong("appSystemId"));
        }

        /*拼凑双层表头theadList*/
        JSONArray finalTheadList = JSONArray.parseArray(theadList.toString());
        List<JSONObject> envTheadList = new ArrayList<>();
        List<JSONObject> scenarioTheadList = new ArrayList<>();
        //表头：环境权限
        for (DeployAppEnvironmentVo env : envList) {
            JSONObject thead = new JSONObject();
            thead.put("name", env.getName());
            thead.put("displayName", env.getName());
            envTheadList.add(thead);
        }
        finalTheadList.add(new JSONObject() {{
            put("list", envTheadList);
            put("displayName", "环境权限");
        }});
        //表头：场景权限
        for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
            JSONObject thead = new JSONObject();
            thead.put("name", scenarioVo.getScenarioName());
            thead.put("displayName", scenarioVo.getScenarioName());
            scenarioTheadList.add(thead);
        }
        finalTheadList.add(new JSONObject() {{
            put("list", scenarioTheadList);
            put("displayName", "场景权限");
        }});

        //获取tbodyList
        List<JSONObject> bodyList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppConfigAuthorityCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<DeployAppConfigAuthorityVo> appConfigAuthList = deployAppConfigMapper.getAppConfigAuthorityList(searchVo);
            if (CollectionUtils.isNotEmpty(appConfigAuthList)) {

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
