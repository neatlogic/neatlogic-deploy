package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.app.DeployAppConfigVo;

/**
 * @author longrf
 * @date 2022/6/30 2:26 下午
 */
public interface DeployAppConfigService {

    /**
     * 删除发布配置
     *
     * @param configVo configVo
     */
    void deleteAppConfig(DeployAppConfigVo configVo);
}
