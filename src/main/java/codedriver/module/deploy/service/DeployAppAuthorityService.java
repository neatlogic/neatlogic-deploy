package codedriver.module.deploy.service;

import codedriver.framework.deploy.constvalue.DeployAppConfigAction;

public interface DeployAppAuthorityService {



    /**
     * 校验是否拥有单个系统下的单个操作权限
     * @param appSystemId 系统id
     * @param action 操作权限
     */
    void checkOperationAuth(Long appSystemId,  DeployAppConfigAction action);

    /**
     * 校验是否拥有单个系统下的单个环境权限
     * @param appSystemId 系统id
     * @param envId 环境id
     */
    void checkEnvAuth(Long appSystemId, Long envId);


}
