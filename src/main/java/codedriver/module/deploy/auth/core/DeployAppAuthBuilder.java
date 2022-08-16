/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.auth.core;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DeployAppAuthBuilder {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    private static DeployAppAuthBuilder builder;

    @Autowired
    public DeployAppAuthBuilder() {
        builder = this;
    }

//    @Autowired
//    public DeployAppAuthBuilder(DeployAppPipelineService _deployAppPipelineService, DeployAppConfigMapper _deployAppConfigMapper) {
//        deployAppPipelineService = _deployAppPipelineService;
//        deployAppConfigMapper = _deployAppConfigMapper;
//    }

    private static List<String> typeActionList = new ArrayList<>();
    private static Map<Long, Set<String>> typeActionListMap = new HashMap<>();
    private final static List<String> actionTypeList = DeployAppConfigActionType.getValueList();


    public DeployAppAuthBuilder(List<String> typeActionList) {
        DeployAppAuthBuilder.typeActionList = typeActionList;
    }

    public DeployAppAuthBuilder(Map<Long, Set<String>> typeActionListMap) {
        DeployAppAuthBuilder.typeActionListMap = typeActionListMap;
    }

    public static DeployAppAuthBuilder addOperationAction(String operationString) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);
        typeActionList.add(DeployAppConfigActionType.OPERATION.getValue() + "#" + operationString);
        return authBuilder;
    }

    public static DeployAppAuthBuilder addEnvAction(Long envId) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);
        typeActionList.add(DeployAppConfigActionType.ENV.getValue() + "#" + envId);
        return authBuilder;
    }

    public static DeployAppAuthBuilder addScenarioAction(Long scenarioId) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);
        typeActionList.add(DeployAppConfigActionType.ENV.getValue() + "#" + scenarioId);
        return authBuilder;
    }

    public static DeployAppAuthBuilder addOperationActionList(List<String> operationStringList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);

        for (String operationString : operationStringList) {
            typeActionList.add(DeployAppConfigActionType.OPERATION.getValue() + "#" + operationString);
        }
        return authBuilder;
    }

    public static DeployAppAuthBuilder addEnvActionList(List<Long> envIdList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);

        for (Long envId : envIdList) {
            typeActionList.add(DeployAppConfigActionType.ENV.getValue() + "#" + envId);
        }
        return authBuilder;
    }

    public static DeployAppAuthBuilder addScenarioActionList(List<Long> scenarioIdList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionList);

        for (Long scenarioId : scenarioIdList) {
            typeActionList.add(DeployAppConfigActionType.SCENARIO.getValue() + "#" + scenarioId);
        }
        return authBuilder;
    }

    public static DeployAppAuthBuilder addOperationActionMap(Long appSystemId, List<String> operationList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionListMap);
        Set<String> oldOperationSet = typeActionListMap.get(appSystemId);
        Set<String> paramOperationSet = new HashSet<>();
        for (String operation : operationList) {
            paramOperationSet.add(DeployAppConfigActionType.OPERATION.getValue() + "#" + operation);
        }
        if (CollectionUtils.isNotEmpty(oldOperationSet)) {
            oldOperationSet.addAll(paramOperationSet);
        } else {
            typeActionListMap.put(appSystemId, paramOperationSet);
        }
        return authBuilder;
    }

    public static DeployAppAuthBuilder addEnvActionMap(Long appSystemId, List<Long> envIdList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionListMap);
        Set<String> oldEnvIdSet = typeActionListMap.get(appSystemId);
        Set<String> paramEnvSet = new HashSet<>();
        for (Long envId : envIdList) {
            paramEnvSet.add(DeployAppConfigActionType.ENV.getValue() + "#" + envId);
        }
        if (CollectionUtils.isNotEmpty(oldEnvIdSet)) {
            oldEnvIdSet.addAll(paramEnvSet);
        } else {
            typeActionListMap.put(appSystemId, paramEnvSet);
        }
        return authBuilder;
    }

    public static DeployAppAuthBuilder addScenarioActionMap(Long appSystemId, List<Long> scenarioIdList) {
        DeployAppAuthBuilder authBuilder = new DeployAppAuthBuilder(typeActionListMap);
        Set<String> oldScenarioSet = typeActionListMap.get(appSystemId);
        Set<String> paramScenarioIdSet = new HashSet<>();
        for (Long scenarioId : scenarioIdList) {
            paramScenarioIdSet.add(DeployAppConfigActionType.SCENARIO.getValue() + "#" + scenarioId);
        }
        if (CollectionUtils.isNotEmpty(oldScenarioSet)) {
            oldScenarioSet.addAll(paramScenarioIdSet);
        } else {
            typeActionListMap.put(appSystemId, paramScenarioIdSet);
        }
        return authBuilder;
    }


    public Set<String> builder(Long appSystemId) {
        Set<String> returnActionSet = new HashSet<>();

        if (appSystemId == null || CollectionUtils.isEmpty(typeActionList)) {
            return returnActionSet;
        }
        DeployAppAuthCheckVo checkVo = new DeployAppAuthCheckVo(appSystemId, new HashSet<>(typeActionList));
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            return new HashSet<>(typeActionList);
        }
        if (CollectionUtils.isEmpty(builder.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId))) {
            return new HashSet<>(typeActionList);
        }
        checkVo.setAuthorityActionList(new ArrayList<>(DeployAppConfigActionType.getActionList(checkVo.getAuthorityActionList())));
        List<DeployAppConfigAuthorityActionVo> hasActionList = builder.deployAppConfigMapper.getDeployAppAuthorityActionList(checkVo);
        if (CollectionUtils.isEmpty(hasActionList)) {
            return returnActionSet;
        }
        return getActionSet(DeployAppConfigActionType.getActionVoList(typeActionList), hasActionList);
    }

    public Map<Long, Set<String>> batchBuilder() {

        HashMap<Long, Set<String>> returnMap = new HashMap<>();
        if (MapUtils.isEmpty(typeActionListMap)) {
            return returnMap;
        }

        /*将其分类为有特权和无特权（发布管理员权限和没有配置过的系统）两种，有特权直接拼接需要验权的权限列表到returnMap里，无特权的用sql语句进行批量验权，再拼接数据到returnMap里*/

        //1、查询已经配置过权限的系统id列表
        List<Long> hasConfigAuthAppSystemIdList = builder.deployAppConfigMapper.getDeployAppHasAuthorityAppSystemIdListByAppSystemIdList(typeActionListMap.keySet());
        //声明需要验权限的checkVo列表
        List<DeployAppAuthCheckVo> needCheckAuthCheckList = new ArrayList<>();
        //2、循环入参系统id，将其分类为有特权和无特权两种
        for (Long paramAppSystemId : typeActionListMap.keySet()) {
            if (!hasConfigAuthAppSystemIdList.contains(paramAppSystemId)) {
                //没有配置过权限的
                returnMap.put(paramAppSystemId, DeployAppConfigActionType.getActionList(new ArrayList<>(typeActionListMap.get(paramAppSystemId))));
            } else if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                //拥有发布管理员特权的
                returnMap.put(paramAppSystemId, DeployAppConfigActionType.getActionList(new ArrayList<>(typeActionListMap.get(paramAppSystemId))));
            } else {
                //单个验权的
                needCheckAuthCheckList.add(new DeployAppAuthCheckVo(paramAppSystemId, new ArrayList<>(DeployAppConfigActionType.getActionVoList(new ArrayList<>(typeActionListMap.get(paramAppSystemId))))));
            }
        }

        //3、批量查询是否拥有多个系统的多种权限，并用sql返回，再循环拼接到returnMap里
        List<DeployAppAuthCheckVo> hasConfigAuthorityCheckVoList = builder.deployAppConfigMapper.getBatchDeployAppAuthorityActionList(needCheckAuthCheckList);
        Map<Long, List<DeployAppConfigAuthorityActionVo>> hasConfigAuthorityActionMap = hasConfigAuthorityCheckVoList.stream().collect(Collectors.toMap(DeployAppAuthCheckVo::getAppSystemId, DeployAppAuthCheckVo::getActionVoList));
        for (DeployAppAuthCheckVo checkVo : needCheckAuthCheckList) {
            returnMap.put(checkVo.getAppSystemId(), getActionSet(checkVo.getActionVoList(), hasConfigAuthorityActionMap.get(checkVo.getAppSystemId())));
        }
        return returnMap;
    }

    /**
     * 取 所需权限 和 现有权限 的交集
     *
     * @param needCheckActionList 所需权限
     * @param hasActionList       现有权限
     * @return 所需权限和现有权限的交集
     */
    private static Set<String> getActionSet(List<DeployAppConfigAuthorityActionVo> needCheckActionList, List<DeployAppConfigAuthorityActionVo> hasActionList) {
        Set<String> returnActionSet = new HashSet<>();
        if (CollectionUtils.isEmpty(hasActionList)) {
            return returnActionSet;
        }

        Map<String, List<DeployAppConfigAuthorityActionVo>> needAuthorityActionVoTypeMap = needCheckActionList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
        Map<String, List<DeployAppConfigAuthorityActionVo>> hasAuthorityActionVoTypeMap = hasActionList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));

        List<String> allActionTypeList = new ArrayList<>();
        for (String actionType : actionTypeList) {
            List<DeployAppConfigAuthorityActionVo> actionTypeActionVoList = hasAuthorityActionVoTypeMap.get(actionType);
            if (CollectionUtils.isEmpty(actionTypeActionVoList)) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(actionTypeActionVoList.stream().filter(e -> StringUtils.equals(e.getAction(), "all")).collect(Collectors.toList()))) {
                allActionTypeList.add(actionType);
                if (StringUtils.equals(actionType, actionType) && CollectionUtils.isNotEmpty(needAuthorityActionVoTypeMap.get(actionType))) {
                    returnActionSet.addAll(needAuthorityActionVoTypeMap.get(actionType).stream().map(DeployAppConfigAuthorityActionVo::getAction).collect(Collectors.toList()));
                }
            }
        }

        for (DeployAppConfigAuthorityActionVo actionVo : hasActionList) {
            if (!allActionTypeList.contains(actionVo.getType())) {
                returnActionSet.add(actionVo.getAction());
            }
        }
        return returnActionSet;
    }


    /**
     * 根据系统id获取当前登录人所有权限
     *
     * @param appSystemId 系统id
     * @return
     */
    public static JSONObject getAppConfigAuthorityList(Long appSystemId) {
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
            List<DeployAppConfigAuthorityVo> systemAuthList = builder.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
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

            List<DeployAppConfigAuthorityActionVo> hasAllAuthorityList = builder.deployAppConfigMapper.getDeployAppAllAuthorityActionListByAppSystemIdAndAuthUuidList(appSystemId, authUuidList);
            if (CollectionUtils.isNotEmpty(hasAllAuthorityList)) {

                Map<String, List<DeployAppConfigAuthorityActionVo>> hasAuthorityActionVoTypeMap = hasAllAuthorityList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
                List<String> allActionTypeList = new ArrayList<>();
                for (String actionType : actionTypeList) {
                    List<DeployAppConfigAuthorityActionVo> actionTypeActionVoList = hasAuthorityActionVoTypeMap.get(actionType);
                    if (CollectionUtils.isEmpty(actionTypeActionVoList)) {
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(actionTypeActionVoList.stream().filter(e -> StringUtils.equals(e.getAction(), "all")).collect(Collectors.toList()))) {


                        allActionTypeList.add(actionType);
                        if (StringUtils.equals(actionType, DeployAppConfigActionType.OPERATION.getValue())) {
                            operationAuthList.addAll(DeployAppConfigAction.getValueList());
                        } else if (StringUtils.equals(actionType, DeployAppConfigActionType.ENV.getValue())) {
                            List<DeployAppEnvironmentVo> envVoList = builder.deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName());
                            if (CollectionUtils.isNotEmpty(envVoList)) {
                                for (DeployAppEnvironmentVo envVo : envVoList) {
                                    envAuthList.add(envVo.getId().toString());
                                }
                            }
                        } else if (StringUtils.equals(actionType, DeployAppConfigActionType.SCENARIO.getValue())) {
                            DeployPipelineConfigVo pipelineConfigVo = builder.deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
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
    private static JSONObject getAllAuthority(Long appSystemId) {
        JSONObject returnObj = new JSONObject();
        //操作权限
        returnObj.put("operationAuthList", DeployAppConfigAction.getValueList());
        //环境权限
        List<String> envAuthList = new ArrayList<>();
        for (DeployAppEnvironmentVo env : builder.deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName())) {
            envAuthList.add(env.getId().toString());
        }
        returnObj.put("envAuthList", envAuthList);
        //场景权限
        DeployPipelineConfigVo pipelineConfigVo = builder.deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
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
