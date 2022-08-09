package codedriver.module.deploy.auth.core;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeployAppAuthChecker {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Autowired
    private DeployAppConfigMapper deployAppConfigMapper;

    private static DeployAppAuthChecker checker;

    @Autowired
    public DeployAppAuthChecker() {
        checker = this;
    }

    //发布管理员最高权限

    //如果当前系统配置了权限，则校验权限，否则所有人都是拥有所有权限

    // 1）创建作业、执行作业：需要校验 环境权限&场景权限
    //    2）查看作业：查看作业、配置权限
    //    3）应用配置查看：查看作业、配置权限
    //    4）配置修改：
    //        A.应用层： 编辑配置权限
    //        B.模块层： 编辑配置权限
    //        C.环境层： 编辑配置权限&环境权限
    //    5）版本制品：制品管理权限
    //    6）环境制品：制品管理权限&环境权限


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
                if (hasScenarioPrivilege(appSystemId, scenarioVo.getScenarioName())) {
                    scenarioAuthList.add(scenarioVo.getScenarioName());
                }
            }
            returnObj.put("scenarioAuthList", scenarioAuthList);
        }
        return returnObj;
    }

    /**
     * 批量校验是否拥有这些权限
     */


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
            scenarioAuthList.add(scenarioVo.getScenarioName());
        }
        returnObj.put("scenarioAuthList", scenarioAuthList);
        return returnObj;
    }
}
