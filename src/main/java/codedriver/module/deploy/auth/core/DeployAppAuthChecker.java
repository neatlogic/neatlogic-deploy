package codedriver.module.deploy.auth.core;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeployAppAuthChecker {

    @Autowired
    private TeamMapper teamMapper;

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


    //校验是否拥有操作权限
    private static boolean hasOperationPrivilege(Long appSystemId, DeployAppConfigAction action) {
        if (appSystemId != null) {
            boolean hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    return true;
                }

                //对该权限已配置的权限列表
                List<DeployAppConfigAuthorityVo> actionAuthList = systemAuthList.stream().filter(e -> (StringUtils.equals(action.getValue(), e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getActionType(), DeployAppConfigActionType.OPERATION.getValue())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(actionAuthList)) {
                    return false;
                }

                String userUuid = UserContext.get().getUserUuid(true);
                List<String> roleUuidList = UserContext.get().getRoleUuidList();
                List<String> teamUuidList = checker.teamMapper.getTeamUuidListByUserUuid(userUuid);

                for (DeployAppConfigAuthorityVo authorityVo : actionAuthList) {
                    switch (authorityVo.getAuthType()) {
                        case "common":
                            if (authorityVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                                return true;
                            }
                            break;
                        case "user":
                            if (userUuid.equals(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "team":
                            if (teamUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "role":
                            if (roleUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                    }
                }
            }
        }
        return false;

    }

    private static boolean hasPrivilege(Long appSystemId, DeployAppConfigAction action) {
        if (appSystemId != null) {
            //访问系统需要的权限
            List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
            if (CollectionUtils.isEmpty(systemAuthList)) {
                return true;
            }

            //对该权限已配置的权限列表
            List<DeployAppConfigAuthorityVo> actionAuthList = systemAuthList.stream().filter(e -> (StringUtils.equals(action.getValue(), e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getActionType(), DeployAppConfigActionType.OPERATION.getValue())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(actionAuthList)) {
                return false;
            }

            String userUuid = UserContext.get().getUserUuid(true);
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<String> teamUuidList = checker.teamMapper.getTeamUuidListByUserUuid(userUuid);

            for (DeployAppConfigAuthorityVo authorityVo : actionAuthList) {
                switch (authorityVo.getAuthType()) {
                    case "common":
                        if (authorityVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                            return true;
                        }
                        break;
                    case "user":
                        if (userUuid.equals(authorityVo.getAuthUuid())) {
                            return true;
                        }
                        break;
                    case "team":
                        if (teamUuidList.contains(authorityVo.getAuthUuid())) {
                            return true;
                        }
                        break;
                    case "role":
                        if (roleUuidList.contains(authorityVo.getAuthUuid())) {
                            return true;
                        }
                        break;
                }
            }
        }
        return false;
    }


    //校验是否拥有环境权限
    private static boolean hasEnvPrivilege(Long appSystemId, Long envAction) {
        if (appSystemId != null) {
            boolean hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    return true;
                }

                //对该权限已配置的权限列表
                List<DeployAppConfigAuthorityVo> actionAuthList = systemAuthList.stream().filter(e -> (StringUtils.equals(envAction.toString(), e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getActionType(), DeployAppConfigActionType.ENV.getValue())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(actionAuthList)) {
                    return false;
                }

                String userUuid = UserContext.get().getUserUuid(true);
                List<String> roleUuidList = UserContext.get().getRoleUuidList();
                List<String> teamUuidList = checker.teamMapper.getTeamUuidListByUserUuid(userUuid);

                for (DeployAppConfigAuthorityVo authorityVo : actionAuthList) {
                    switch (authorityVo.getAuthType()) {
                        case "common":
                            if (authorityVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                                return true;
                            }
                            break;
                        case "user":
                            if (userUuid.equals(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "team":
                            if (teamUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "role":
                            if (roleUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                    }
                }
            }
        }
        return false;

    }


    //校验是否拥有场景权限

    //校验是否拥有环境权限
    private static boolean hasScenarioPrivilege(Long appSystemId, String scenarioAction) {
        if (appSystemId != null) {
            boolean hasAuth = AuthActionChecker.check(DEPLOY_MODIFY.class);
            if (!hasAuth) {
                //访问系统需要的权限
                List<DeployAppConfigAuthorityVo> systemAuthList = checker.deployAppConfigMapper.getAppConfigAuthorityListByAppSystemId(appSystemId);
                if (CollectionUtils.isEmpty(systemAuthList)) {
                    return true;
                }

                //对该权限已配置的权限列表
                List<DeployAppConfigAuthorityVo> actionAuthList = systemAuthList.stream().filter(e -> (StringUtils.equals(scenarioAction, e.getAction()) || StringUtils.equals(e.getAction(), "all")) && StringUtils.equals(e.getActionType(), DeployAppConfigActionType.SCENARIO.getValue())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(actionAuthList)) {
                    return false;
                }

                String userUuid = UserContext.get().getUserUuid(true);
                List<String> roleUuidList = UserContext.get().getRoleUuidList();
                List<String> teamUuidList = checker.teamMapper.getTeamUuidListByUserUuid(userUuid);

                for (DeployAppConfigAuthorityVo authorityVo : actionAuthList) {
                    switch (authorityVo.getAuthType()) {
                        case "common":
                            if (authorityVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                                return true;
                            }
                            break;
                        case "user":
                            if (userUuid.equals(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "team":
                            if (teamUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                        case "role":
                            if (roleUuidList.contains(authorityVo.getAuthUuid())) {
                                return true;
                            }
                            break;
                    }
                }
            }
        }
        return false;

    }
}
