package codedriver.module.deploy.service;

import codedriver.framework.deploy.constvalue.DeployAppConfigAction;

import java.util.List;

public interface DeployAppAuthorityService {


    /**
     * 校验是否拥有单个系统下的单个操作权限
     *
     * @param appSystemId 系统id
     * @param action      操作权限
     */
    void checkOperationAuth(Long appSystemId, DeployAppConfigAction action);

    /**
     * 校验是否拥有单个系统下的单个环境权限
     *
     * @param appSystemId 系统id
     * @param envId       环境id
     */
    void checkEnvAuth(Long appSystemId, Long envId);

    /**
     * 校验是否拥有单个系统下的单个场景权限
     *
     * @param appSystemId 系统id
     * @param scenarioId  场景id
     */
    void checkScenarioAuth(Long appSystemId, Long scenarioId);

    /**
     * 校验是否拥有单个系统下的多个环境权限
     *
     * @param appSystemId 系统id
     * @param envIdList   环境id列表
     */
    void checkEnvAuthList(Long appSystemId, List<Long> envIdList);


}
