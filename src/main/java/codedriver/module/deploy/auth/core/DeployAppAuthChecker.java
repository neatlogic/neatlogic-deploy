/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.auth.core;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DeployAppAuthChecker {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    private static DeployAppAuthChecker checker;

    @Autowired
    public DeployAppAuthChecker() {
        checker = this;
    }

    //权限类型
    private final static List<String> actionTypeList = DeployAppConfigActionType.getValueList();

    /**
     * 校验单个系统权限的构造方法
     *
     * @param appSystemId 系统id
     * @return Builder
     */
    public static Builder builder(Long appSystemId) {
        return new Builder(appSystemId);
    }

    /**
     * 校验多的系统权限的构造方法
     *
     * @return BatchBuilder
     */
    public static BatchBuilder batchbuilder() {
        return new BatchBuilder();
    }

    //校验单个系统权限内部类
    public static class Builder {

        //校验单系统权限时配合check()使用
        private final List<String> typeActionList = new ArrayList<>();
        private final Long appSystemId;

        public Builder(Long appSystemId) {
            this.appSystemId = appSystemId;
        }

        public Builder addOperationAction(String operationString) {
            typeActionList.add(operationString);
            return this;
        }

        public Builder addEnvAction(Long envId) {
            typeActionList.add(envId.toString());
            return this;
        }

        public Builder addScenarioAction(Long scenarioId) {
            typeActionList.add(scenarioId.toString());
            return this;
        }

        public Builder addOperationActionList(List<String> operationStringList) {
            typeActionList.addAll(operationStringList);
            return this;
        }

        public Builder addEnvActionList(List<Long> envIdList) {
            for (Long envId : envIdList) {
                typeActionList.add(envId.toString());
            }
            return this;
        }

        public Builder addScenarioActionList(List<Long> scenarioIdList) {
            for (Long scenarioId : scenarioIdList) {
                typeActionList.add(scenarioId.toString());
            }
            return this;
        }

        public Set<String> check() {
            return DeployAppAuthChecker.check(appSystemId, typeActionList);
        }

    }

    //批量校验多个系统权限内部类
    public static class BatchBuilder {

        //校验多系统时batchCheck()使用
        private final Map<Long, Set<String>> typeActionSetMap = new HashMap<>();

        public BatchBuilder addOperationActionMap(Long appSystemId, List<String> operationList) {
            Set<String> oldOperationSet = typeActionSetMap.get(appSystemId);
            Set<String> paramOperationSet = new HashSet<>(operationList);
            if (CollectionUtils.isNotEmpty(oldOperationSet)) {
                oldOperationSet.addAll(paramOperationSet);
            } else {
                typeActionSetMap.put(appSystemId, paramOperationSet);
            }
            return this;
        }

        public BatchBuilder addEnvActionMap(Long appSystemId, List<Long> envIdList) {
            Set<String> oldEnvIdSet = typeActionSetMap.get(appSystemId);
            Set<String> paramEnvSet = new HashSet<>();
            for (Long envId : envIdList) {
                paramEnvSet.add(envId.toString());
            }
            if (CollectionUtils.isNotEmpty(oldEnvIdSet)) {
                oldEnvIdSet.addAll(paramEnvSet);
            } else {
                typeActionSetMap.put(appSystemId, paramEnvSet);
            }
            return this;
        }

        public BatchBuilder addScenarioActionMap(Long appSystemId, List<Long> scenarioIdList) {
            Set<String> oldScenarioSet = typeActionSetMap.get(appSystemId);
            Set<String> paramScenarioIdSet = new HashSet<>();
            for (Long scenarioId : scenarioIdList) {
                paramScenarioIdSet.add(scenarioId.toString());
            }
            if (CollectionUtils.isNotEmpty(oldScenarioSet)) {
                oldScenarioSet.addAll(paramScenarioIdSet);
            } else {
                typeActionSetMap.put(appSystemId, paramScenarioIdSet);
            }
            return this;
        }

        public Map<Long, Set<String>> batchCheck() {
            return DeployAppAuthChecker.batchCheck(typeActionSetMap);
        }
    }

    /**
     * 校验单个系统权限
     *
     * @param appSystemId    系统id
     * @param typeActionList 权限列表
     * @return 拥有的权限列表
     */
    private static Set<String> check(Long appSystemId, List<String> typeActionList) {

        /*发布管理员拥有所有权限*/
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            return getAllAuthInfo(appSystemId);
        }

        List<DeployAppConfigAuthorityVo> appSystemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
        /*如果当前系统没有配置权限，则所有人均拥有所有权限*/
        if (CollectionUtils.isEmpty(appSystemAuthList)) {
            return getAllAuthInfo(appSystemId);
        }

        //获取场景id列表
        DeployPipelineConfigVo pipelineConfigVo = new DeployPipelineConfigVo();
        if (CollectionUtils.isNotEmpty(checker.deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId))) {
            pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId).getConfig();
        }
        List<Long> scenarioIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
            scenarioIdList = pipelineConfigVo.getScenarioList().stream().map(AutoexecCombopScenarioVo::getScenarioId).collect(Collectors.toList());
        }


        return new HashSet<>(CollectionUtils.intersection(typeActionList, getHasAuthoritySet(appSystemId, checker.deployAppConfigMapper.getDeployAppEnvIdListByAppSystemId(appSystemId, TenantContext.get().getDataDbName()), scenarioIdList, appSystemAuthList)));

    }

    /**
     * 批量校验多系统下的多权限
     *
     * @param typeActionSetMap 需要校验的map
     * @return 拥有的权限map
     */
    private static Map<Long, Set<String>> batchCheck(Map<Long, Set<String>> typeActionSetMap) {

        HashMap<Long, Set<String>> returnMap = new HashMap<>();
        if (MapUtils.isEmpty(typeActionSetMap)) {
            return returnMap;
        }

        //批量获取系统的环境列表
        List<DeployAppSystemVo> appSystemVoListIncludeEnvIdList = checker.deployAppConfigMapper.getDeployAppSystemListIncludeEnvIdListByAppSystemIdList(new ArrayList<>(typeActionSetMap.keySet()), TenantContext.get().getDataDbName());
        Map<Long, List<Long>> appSystemIdEnvIdListMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(appSystemVoListIncludeEnvIdList)) {
            appSystemIdEnvIdListMap = appSystemVoListIncludeEnvIdList.stream().collect(Collectors.toMap(DeployAppSystemVo::getId, DeployAppSystemVo::getEnvIdList));
        }

        /*将其分类为有特权和无特权（发布管理员权限和没有配置过的系统）两种，有特权直接拼接需要验权的权限列表到returnMap里，无特权的用sql语句进行批量验权，再拼接数据到returnMap里*/

        //1、查询已经配置过权限的系统id列表
        List<Long> hasConfigAuthAppSystemIdList = checker.deployAppConfigMapper.getDeployAppHasAuthorityAppSystemIdListByAppSystemIdList(typeActionSetMap.keySet());
        List<DeployAppSystemVo> needCheckAppSystemVoList = checker.deployAppConfigMapper.getBatchAppConfigAuthorityListByAppSystemIdList(new ArrayList<>(typeActionSetMap.keySet()));

        //声明需要验权限的checkVo列表

        List<Long> needCheckAuthAppSystemIdList = new ArrayList<>();
        //2、循环入参系统id，将其分类为有特权和无特权两种
        for (DeployAppSystemVo appSystemVo : needCheckAppSystemVoList) {
            List<DeployAppConfigAuthorityVo> appSystemAuthList = appSystemVo.getAuthList();
            /*如果当前系统没有配置权限，则所有人均拥有所有权限*/
            if (CollectionUtils.isNotEmpty(appSystemAuthList) && appSystemAuthList.size() == 1 && Objects.isNull(appSystemAuthList.get(0).getAuthUuid())) {
                //没有配置过权限的
                returnMap.put(appSystemVo.getId(), DeployAppConfigActionType.getActionList(new ArrayList<>(typeActionSetMap.get(appSystemVo.getId()))));
            } else if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                //拥有发布管理员特权的
                returnMap.put(appSystemVo.getId(), DeployAppConfigActionType.getActionList(new ArrayList<>(typeActionSetMap.get(appSystemVo.getId()))));
            } else {
                //单个验权的
                List<Long> scenarioIdList = new ArrayList<>();
                DeployPipelineConfigVo pipelineConfigVo = new DeployPipelineConfigVo();
                if (CollectionUtils.isNotEmpty(checker.deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemVo.getId()))) {
                    pipelineConfigVo = DeployPipelineConfigManager.init(appSystemVo.getId()).getConfig();
                }
                if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
                    scenarioIdList = pipelineConfigVo.getScenarioList().stream().map(AutoexecCombopScenarioVo::getScenarioId).collect(Collectors.toList());
                }
                returnMap.put(appSystemVo.getId(), new HashSet<>(CollectionUtils.intersection(typeActionSetMap.get(appSystemVo.getId()), getHasAuthoritySet(appSystemVo.getId(), appSystemIdEnvIdListMap.get(appSystemVo.getId()), scenarioIdList, appSystemVo.getAuthList()))));
            }
        }

//        //3、批量查询是否拥有多个系统的多种权限，并用sql返回，再循环拼接到returnMap里
//        List<DeployAppAuthCheckVo> hasConfigAuthorityCheckVoList = checker.deployAppConfigMapper.getBatchDeployAppAuthorityActionList(needCheckAuthCheckList);
//        Map<Long, List<DeployAppConfigAuthorityActionVo>> hasConfigAuthorityActionMap = hasConfigAuthorityCheckVoList.stream().collect(Collectors.toMap(DeployAppAuthCheckVo::getAppSystemId, DeployAppAuthCheckVo::getActionVoList));
//        for (DeployAppAuthCheckVo checkVo : needCheckAuthCheckList) {
//            returnMap.put(checkVo.getAppSystemId(), getActionSet(checkVo.getActionVoList(), hasConfigAuthorityActionMap.get(checkVo.getAppSystemId())));
//        }
        return returnMap;
    }

//    /**
//     * 取 所需权限 和 现有权限 的交集
//     *
//     * @param needCheckActionList 所需权限
//     * @param hasActionList       现有权限
//     * @return 所需权限和现有权限的交集
//     */
//    private static Set<String> getActionSet(List<DeployAppConfigAuthorityActionVo> needCheckActionList, List<DeployAppConfigAuthorityActionVo> hasActionList) {
//        Set<String> returnActionSet = new HashSet<>();
//        if (CollectionUtils.isEmpty(hasActionList)) {
//            return returnActionSet;
//        }
//
//        Map<String, List<DeployAppConfigAuthorityActionVo>> needAuthorityActionVoTypeMap = needCheckActionList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
//        Map<String, List<DeployAppConfigAuthorityActionVo>> hasAuthorityActionVoTypeMap = hasActionList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
//
//        List<String> allActionTypeList = new ArrayList<>();
//        for (String actionType : actionTypeList) {
//            List<DeployAppConfigAuthorityActionVo> actionTypeActionVoList = hasAuthorityActionVoTypeMap.get(actionType);
//            if (CollectionUtils.isEmpty(actionTypeActionVoList)) {
//                continue;
//            }
//            if (CollectionUtils.isNotEmpty(actionTypeActionVoList.stream().filter(e -> StringUtils.equals(e.getAction(), "all")).collect(Collectors.toList()))) {
//                allActionTypeList.add(actionType);
//                if (StringUtils.equals(actionType, actionType) && CollectionUtils.isNotEmpty(needAuthorityActionVoTypeMap.get(actionType))) {
//                    returnActionSet.addAll(needAuthorityActionVoTypeMap.get(actionType).stream().map(DeployAppConfigAuthorityActionVo::getAction).collect(Collectors.toList()));
//                }
//            }
//        }
//
//        for (DeployAppConfigAuthorityActionVo actionVo : hasActionList) {
//            if (!allActionTypeList.contains(actionVo.getType())) {
//                returnActionSet.add(actionVo.getAction());
//            }
//        }
//        return returnActionSet;
//    }


    public static Map<Long, Set<String>> getAppConfigAuthorityList(Map<Long, List<DeployAppConfigAuthorityVo>> appSystemIdAuthListMap) {

        //批量获取系统的环境列表
        List<DeployAppSystemVo> appSystemVoListIncludeEnvIdList = checker.deployAppConfigMapper.getDeployAppSystemListIncludeEnvIdListByAppSystemIdList(new ArrayList<>(appSystemIdAuthListMap.keySet()), TenantContext.get().getDataDbName());
        Map<Long, List<Long>> appSystemIdEnvIdListMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(appSystemVoListIncludeEnvIdList)) {
            appSystemIdEnvIdListMap = appSystemVoListIncludeEnvIdList.stream().collect(Collectors.toMap(DeployAppSystemVo::getId, DeployAppSystemVo::getEnvIdList));
        }

        Map<Long, Set<String>> returnMap = new HashMap<>();
        for (Long appSystemId : appSystemIdAuthListMap.keySet()) {
            List<Long> scenarioIdList = new ArrayList<>();
            DeployPipelineConfigVo pipelineConfigVo = new DeployPipelineConfigVo();
            if (CollectionUtils.isNotEmpty(checker.deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId))) {
                pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId).getConfig();
            }
            if (CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
                scenarioIdList = pipelineConfigVo.getScenarioList().stream().map(AutoexecCombopScenarioVo::getScenarioId).collect(Collectors.toList());
            }
            returnMap.put(appSystemId, getHasAuthoritySet(appSystemId, appSystemIdEnvIdListMap.get(appSystemId), scenarioIdList, appSystemIdAuthListMap.get(appSystemId)));
        }
        return returnMap;
    }

    private static Set<String> getHasAuthoritySet(Long appSystemId, List<Long> envIdList, List<Long> scenarioIdList, List<DeployAppConfigAuthorityVo> appSystemAuthList) {
        Set<String> returnActionList = new HashSet<>();

        /*发布管理员拥有所有权限*/
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            return getAllAuthInfo(appSystemId);
        }

        /*如果当前系统没有配置权限，则所有人均拥有所有权限*/
        if (CollectionUtils.isNotEmpty(appSystemAuthList) && appSystemAuthList.size() == 1 && Objects.isNull(appSystemAuthList.get(0).getAuthUuid())) {
            return getAllAuthInfo(appSystemId);
        }

        //分组角色信息
        AuthenticationInfoVo authInfo = UserContext.get().getAuthenticationInfoVo();
        //循环已配置的权限列表，解析权限，并构造结构
        for (DeployAppConfigAuthorityVo authVo : appSystemAuthList) {
            if (authInfo.getUserUuid().equals(authVo.getAuthUuid()) || authInfo.getRoleUuidList().contains(authVo.getAuthUuid()) || authInfo.getTeamUuidList().contains(authVo.getAuthUuid()) || StringUtils.equals(UserType.ALL.getValue(), authVo.getAuthUuid())) {
                for (DeployAppConfigAuthorityActionVo actionVo : authVo.getActionList()) {
                    if (StringUtils.equals(actionVo.getAction(), "all")) {
                        if (StringUtils.equals(DeployAppConfigActionType.OPERATION.getValue(), actionVo.getType())) {
                            returnActionList.addAll(DeployAppConfigAction.getValueList());
                        } else if (StringUtils.equals(DeployAppConfigActionType.ENV.getValue(), actionVo.getType())) {
                            if (CollectionUtils.isNotEmpty(envIdList)) {
                                for (Long envId : envIdList) {
                                    returnActionList.add(envId.toString());
                                }
                            }
                        } else if (StringUtils.equals(DeployAppConfigActionType.SCENARIO.getValue(), actionVo.getType())) {
                            if (CollectionUtils.isNotEmpty(envIdList)) {
                                for (Long scenarioId : scenarioIdList) {
                                    returnActionList.add(scenarioId.toString());
                                }
                            }
                        }
                    } else {
                        returnActionList.add(actionVo.getAction());
                    }
                }
            }
        }
        return returnActionList;
    }

    /**
     * 根据系统id获取所有权限
     *
     * @param appSystemId 系统id
     * @return 所有权限信息
     */
    private static Set<String> getAllAuthInfo(Long appSystemId) {
        //操作权限
        Set<String> authActionSet = new HashSet<>(DeployAppConfigAction.getValueList());
        //环境权限
        for (DeployAppEnvironmentVo env : checker.deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName())) {
            authActionSet.add(env.getId().toString());
        }
        //场景权限
        DeployPipelineConfigVo pipelineConfigVo = new DeployPipelineConfigVo();
        if (CollectionUtils.isNotEmpty(checker.deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId))) {
            pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId).getConfig();
        }
        if (pipelineConfigVo != null && CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())) {
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                authActionSet.add(scenarioVo.getScenarioId().toString());
            }
        }
        return authActionSet;
    }
}
