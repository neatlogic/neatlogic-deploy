/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigAuthorityApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;
    
    @Override
    public String getName() {
        return "获取当前登录人的应用权限配置";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/authority/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id")
    })
    @Output({
    })
    @Description(desc = "获取当前登录人的应用权限配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        JSONObject returnObj = new JSONObject();
        List<String> operationAuthList = new ArrayList<>();
        List<String> envAuthList = new ArrayList<>();
        List<String> scenarioAuthList = new ArrayList<>();

        if (appSystemId != null) {

            /*发布管理员拥有所有权限*/
            if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                return getAllAuthority(appSystemId);
            }

            /*如果当前系统没有配置权限，则所有人均拥有所有权限*/
            //访问系统需要的权限
            List<DeployAppConfigAuthorityVo> systemAuthList = deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
            if (CollectionUtils.isEmpty(systemAuthList)) {
                return getAllAuthority(appSystemId);
            }

            List<String> authUuidList = new ArrayList<>();
            AuthenticationInfoVo authInfo = UserContext.get().getAuthenticationInfoVo();
            authUuidList.add(authInfo.getUserUuid());
            if (CollectionUtils.isNotEmpty(authInfo.getTeamUuidList())) {
                authUuidList.addAll(authInfo.getTeamUuidList());
            }
            if (CollectionUtils.isNotEmpty(authInfo.getRoleUuidList())) {
                authUuidList.addAll(authInfo.getRoleUuidList());
            }

            List<DeployAppConfigAuthorityActionVo> hasAllAuthorityList = deployAppConfigMapper.getDeployAppAllAuthorityActionListByAppSystemIdAndAuthUuidList(appSystemId, authUuidList);
            if (CollectionUtils.isNotEmpty(hasAllAuthorityList)) {

                Map<String, List<DeployAppConfigAuthorityActionVo>> hasAuthorityActionVoTypeMap = hasAllAuthorityList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
                List<String> allActionTypeList = new ArrayList<>();
                for (String actionType : DeployAppConfigActionType.getValueList()) {
                    List<DeployAppConfigAuthorityActionVo> actionTypeActionVoList = hasAuthorityActionVoTypeMap.get(actionType);
                    if (CollectionUtils.isEmpty(actionTypeActionVoList)) {
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(actionTypeActionVoList.stream().filter(e -> StringUtils.equals(e.getAction(), "all")).collect(Collectors.toList()))) {


                        allActionTypeList.add(actionType);
                        if (StringUtils.equals(actionType, DeployAppConfigActionType.OPERATION.getValue())) {
                            operationAuthList.addAll(DeployAppConfigAction.getValueList());
                        } else if (StringUtils.equals(actionType, DeployAppConfigActionType.ENV.getValue())) {
                            List<DeployAppEnvironmentVo> envVoList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName());
                            if (CollectionUtils.isNotEmpty(envVoList)) {
                                for (DeployAppEnvironmentVo envVo : envVoList) {
                                    envAuthList.add(envVo.getId().toString());
                                }
                            }
                        } else if (StringUtils.equals(actionType, DeployAppConfigActionType.SCENARIO.getValue())) {
                            DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
                            if (pipelineConfigVo == null) {
                                continue;
                            }
                            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                                scenarioAuthList.add(scenarioVo.getScenarioId().toString());
                            }
                        }
                    }
                }

                for (DeployAppConfigAuthorityActionVo actionVo : hasAllAuthorityList) {
                    if (!allActionTypeList.contains(actionVo.getType())) {
                        if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.OPERATION.getValue())) {
                            operationAuthList.add(actionVo.getAction());
                        } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue())) {
                            envAuthList.add(actionVo.getAction());
                        } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue())) {
                            scenarioAuthList.add(actionVo.getAction());
                        }
                    }
                }
            }
        }
        returnObj.put("operationAuthList", operationAuthList);
        returnObj.put("envAuthList", envAuthList);
        returnObj.put("scenarioAuthList", scenarioAuthList);
        return returnObj;
    }

    /**
     * 根据系统id获取所有权限
     *
     * @param appSystemId 系统id
     * @return
     */
    private  JSONObject getAllAuthority(Long appSystemId) {
        JSONObject returnObj = new JSONObject();
        //操作权限
        returnObj.put("operationAuthList", DeployAppConfigAction.getValueList());
        //环境权限
        List<String> envAuthList = new ArrayList<>();
        for (DeployAppEnvironmentVo env : deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName())) {
            envAuthList.add(env.getId().toString());
        }
        returnObj.put("envAuthList", envAuthList);
        //场景权限
        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        List<String> scenarioAuthList = new ArrayList<>();
        for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
            scenarioAuthList.add(scenarioVo.getScenarioId().toString());
        }
        returnObj.put("scenarioAuthList", scenarioAuthList);
        return returnObj;
    }
}
