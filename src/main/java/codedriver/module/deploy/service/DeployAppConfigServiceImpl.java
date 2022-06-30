package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/30 2:26 下午
 */
@Service
public class DeployAppConfigServiceImpl implements DeployAppConfigService {


    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public void deleteAppConfig(DeployAppConfigVo configVo) {

        //删除系统、环境需要删除发布存的环境
        if (deployAppConfigMapper.getAppConfigEnv(configVo) > 0) {
            deployAppConfigMapper.deleteAppConfigEnv(configVo);
        }

        //删除系统才需要删除权限
        if (configVo.getAppModuleId() == 0L && configVo.getEnvId() == 0L ) {
            deployAppConfigMapper.deleteAppConfigAuthorityByAppSystemId(configVo.getAppSystemId());
        }

        deployAppConfigMapper.deleteAppConfig(configVo);
        deployAppConfigMapper.deleteAppConfigDraft(configVo);
        deployAppConfigMapper.deleteAppModuleRunnerGroup(configVo);
        deployAppConfigMapper.deleteAppEnvAutoConfig(new DeployAppEnvAutoConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
    }
}
