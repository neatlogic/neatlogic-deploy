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
public class DeployAppAuthChecker {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    private static DeployAppAuthChecker checker;

    @Autowired
    public DeployAppAuthChecker() {
        checker = this;
    }

    /**
     * 根据系统id获取当前登录人所有权限
     *
     * @param appSystemId 系统id
     * @return
     */
    public static JSONObject getAppConfigAuthorityList(Long appSystemId) {
        JSONObject returnObj = new JSONObject();
        if (appSystemId != null) {

            /*发布管理员拥有所有权限*/
            if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                return getAllAuthority(appSystemId);
            }

            /*如果当前系统没有配置权限，则所有人均拥有所有权限*/
            //访问系统需要的权限
            List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
            if (CollectionUtils.isEmpty(systemAuthList)) {
                return getAllAuthority(appSystemId);
            }

            /*获取当前登录人的所有权限*/
            //1、操作权限
            List<String> operationAuthList = new ArrayList<>();
            for (DeployAppConfigAction action : DeployAppConfigAction.values()) {
                if (hasOperationPrivilege(appSystemId, action)) {
                    operationAuthList.add(action.getValue());
                }
            }
            returnObj.put("operationAuthList", operationAuthList);
            //2、环境权限
            List<String> envAuthList = new ArrayList<>();
            for (DeployAppEnvironmentVo env : checker.deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName())) {
                if (hasEnvPrivilege(appSystemId, env.getId())) {
                    envAuthList.add(env.getId().toString());
                }
            }
            returnObj.put("envAuthList", envAuthList);
            //3、场景权限
            DeployPipelineConfigVo pipelineConfigVo = checker.deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
            if (pipelineConfigVo == null) {
                throw new DeployAppConfigNotFoundException(appSystemId);
            }
            List<String> scenarioAuthList = new ArrayList<>();
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                if (hasScenarioPrivilege(appSystemId, scenarioVo.getScenarioId().toString())) {
                    scenarioAuthList.add(scenarioVo.getScenarioId().toString());
                }
            }
            returnObj.put("scenarioAuthList", scenarioAuthList);
        }
        return returnObj;
    }

    /**
     * 校验是否拥有操作权限
     *
     * @param appSystemId 系统id
     * @param action      操作权限
     * @return
     */
    public static boolean hasOperationPrivilege(Long appSystemId, DeployAppConfigAction action) {
        boolean hasAuth = false;
        if (appSystemId != null) {
            hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    hasAuth = true;
                } else {
                    hasAuth = checkActionAuthList(systemAuthList.stream().filter(e -> (StringUtils.equals(action.getValue(), e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getAuthorityActionType(), DeployAppConfigActionType.OPERATION.getValue())).collect(Collectors.toList()));
                }
            }
        }
        return hasAuth;
    }

    /**
     * 校验是否拥有环境权限
     *
     * @param appSystemId 系统id
     * @param envAction   环境权限
     * @return
     */
    public static boolean hasEnvPrivilege(Long appSystemId, Long envAction) {
        boolean hasAuth = false;
        if (appSystemId != null) {
            hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    hasAuth = true;
                } else {
                    hasAuth = checkActionAuthList(systemAuthList.stream().filter(e -> (StringUtils.equals(envAction.toString(), e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getAuthorityActionType(), DeployAppConfigActionType.ENV.getValue())).collect(Collectors.toList()));
                }
            }
        }
        return hasAuth;
    }

    /**
     * 校验是否拥有场景权限
     *
     * @param appSystemId    系统id
     * @param scenarioAction 场景权限
     * @return
     */
    private static boolean hasScenarioPrivilege(Long appSystemId, String scenarioAction) {
        boolean hasAuth = false;
        if (appSystemId != null) {
            hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    hasAuth = true;
                } else {
                    hasAuth = checkActionAuthList(systemAuthList.stream().filter(e -> (StringUtils.equals(scenarioAction, e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getAuthorityActionType(), DeployAppConfigActionType.SCENARIO.getValue())).collect(Collectors.toList()));
                }
            }
        }
        return hasAuth;
    }

    /**
     * 判断当前登录人是否拥有这些权限
     *
     * @param actionAuthList 权限列表
     * @return
     */
    private static boolean checkActionAuthList(List<DeployAppConfigAuthorityVo> actionAuthList) {
        if (CollectionUtils.isEmpty(actionAuthList)) {
            return false;
        }
        AuthenticationInfoVo authInfo = UserContext.get().getAuthenticationInfoVo();
        for (DeployAppConfigAuthorityVo authorityVo : actionAuthList) {
            switch (authorityVo.getAuthType()) {
                case "common":
                    if (authorityVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                        return true;
                    }
                    break;
                case "user":
                    if (authInfo.getUserUuid().equals(authorityVo.getAuthUuid())) {
                        return true;
                    }
                    break;
                case "team":
                    if (authInfo.getTeamUuidList().contains(authorityVo.getAuthUuid())) {
                        return true;
                    }
                    break;
                case "role":
                    if (authInfo.getRoleUuidList().contains(authorityVo.getAuthUuid())) {
                        return true;
                    }
                    break;
            }
        }
        return false;
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
        for (DeployAppEnvironmentVo env : checker.deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName())) {
            envAuthList.add(env.getId().toString());
        }
        returnObj.put("envAuthList", envAuthList);
        //场景权限
        DeployPipelineConfigVo pipelineConfigVo = checker.deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
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


    /**
     * 校验同一系统的多个权限的权限列表，并返回拥有的权限
     *
     * @param appSystemId     系统id
     * @param paramActionList 校验的权限列表
     * @return 通过校验的权限列表
     */
    public static Set<String> checkAuthorityActionList(Long appSystemId, List<String> paramActionList) {
        Set<String> returnActionSet = new HashSet<>();

        if (appSystemId == null || CollectionUtils.isEmpty(paramActionList)) {
            return returnActionSet;
        }
        DeployAppAuthCheckVo checkVo = new DeployAppAuthCheckVo(appSystemId, new HashSet<>(paramActionList));
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            returnActionSet = new HashSet<>(paramActionList);
        }
        if (CollectionUtils.isEmpty(checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId))) {
            returnActionSet = new HashSet<>(paramActionList);
        }
        List<DeployAppConfigAuthorityActionVo> hasActionList = checker.deployAppConfigMapper.getDeployAppAuthorityActionList(checkVo);
        if (CollectionUtils.isEmpty(hasActionList)) {
            return returnActionSet;
        }
        return getActionSet(DeployAppConfigActionType.getActionVoList(paramActionList), hasActionList);
    }

    /**
     * 校验多个系统的不同权限，并返回拥有的权限
     *
     * @param paramAuthCheckSetMap 入参
     * @return 拥有的权限
     */
    public static Map<Long, Set<String>> checkBatchAuthorityActionList(Map<Long, Set<String>> paramAuthCheckSetMap) {
        HashMap<Long, Set<String>> returnMap = new HashMap<>();
        if (MapUtils.isEmpty(paramAuthCheckSetMap)) {
            return returnMap;
        }

        /*将其分类为有特权和无特权（发布管理员权限和没有配置过的系统）两种，有特权直接拼接需要验权的权限列表到returnMap里，无特权的用sql语句进行批量验权，再拼接数据到returnMap里*/

        //1、查询已经配置过权限的系统id列表
        List<Long> hasConfigAuthAppSystemIdList = checker.deployAppConfigMapper.getDeployAppHasAuthorityAppSystemIdListByAppSystemIdList(paramAuthCheckSetMap.keySet());
        //声明需要验权限的checkVo列表
        List<DeployAppAuthCheckVo> needCheckAuthCheckList = new ArrayList<>();
        //2、循环入参系统id，将其分类为有特权和无特权两种
        for (Long paramAppSystemId : paramAuthCheckSetMap.keySet()) {
            if (!hasConfigAuthAppSystemIdList.contains(paramAppSystemId)) {
                //没有配置过权限的
                returnMap.put(paramAppSystemId, DeployAppConfigActionType.getActionList(new ArrayList<>(paramAuthCheckSetMap.get(paramAppSystemId))));
            } else if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                //拥有发布管理员特权的
                returnMap.put(paramAppSystemId, DeployAppConfigActionType.getActionList(new ArrayList<>(paramAuthCheckSetMap.get(paramAppSystemId))));
            } else {
                //单个验权的
                needCheckAuthCheckList.add(new DeployAppAuthCheckVo(paramAppSystemId, new ArrayList<>(DeployAppConfigActionType.getActionVoList(new ArrayList<>(paramAuthCheckSetMap.get(paramAppSystemId))))));
            }
        }

        //3、批量查询是否拥有多个系统的多种权限，并用sql返回，再循环拼接到returnMap里
        List<DeployAppAuthCheckVo> hasConfigAuthorityCheckVoList = checker.deployAppConfigMapper.getBatchDeployAppAuthorityActionList(needCheckAuthCheckList);
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
        List<String> needCheckActionStringList = needCheckActionList.stream().map(DeployAppConfigAuthorityActionVo::getAction).collect(Collectors.toList());
        for (DeployAppConfigAuthorityActionVo actionVo : hasActionList) {
            if (!StringUtils.equals(actionVo.getAction(), "all")) {
                if (needCheckActionStringList.contains(actionVo.getAction())) {
                    returnActionSet.add(actionVo.getAction());
                }
            } else {
                Map<String, List<DeployAppConfigAuthorityActionVo>> needAuthorityActionVoTypeMap = needCheckActionList.stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
                if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.OPERATION.getValue()) && needAuthorityActionVoTypeMap.containsKey(actionVo.getType())) {
                    returnActionSet.addAll(DeployAppConfigAction.getValueList().stream().filter(needCheckActionStringList::contains).collect(Collectors.toList()));
                } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue()) && needAuthorityActionVoTypeMap.containsKey(actionVo.getType())) {
                    returnActionSet.addAll(needAuthorityActionVoTypeMap.get(DeployAppConfigActionType.ENV.getValue()).stream().map(DeployAppConfigAuthorityActionVo::getAction).collect(Collectors.toList()));
                } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue()) && needAuthorityActionVoTypeMap.containsKey(actionVo.getType())) {
                    returnActionSet.addAll(needAuthorityActionVoTypeMap.get(DeployAppConfigActionType.SCENARIO.getValue()).stream().map(DeployAppConfigAuthorityActionVo::getAction).collect(Collectors.toList()));
                }
            }
        }
        return returnActionSet;
    }

}
