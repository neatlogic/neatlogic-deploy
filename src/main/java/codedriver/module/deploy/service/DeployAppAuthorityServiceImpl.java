/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.service;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.exception.DeployAppEnvAuthException;
import codedriver.framework.deploy.exception.DeployAppOperationAuthException;
import codedriver.module.deploy.auth.core.DeployAppAuthChecker;
import org.springframework.stereotype.Service;

@Service
public class DeployAppAuthorityServiceImpl implements DeployAppAuthorityService{

    @Override
    public void checkOperationAuth(Long appSystemId, DeployAppConfigAction action) {
        if (!DeployAppAuthChecker.hasOperationPrivilege(appSystemId, action)) {
            ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (appSystemCiEntity == null) {
                throw new CiEntityNotFoundException(appSystemId);
            }
            throw new DeployAppOperationAuthException(appSystemCiEntity, action);
        }
    }

    @Override
    public void checkEnvAuth(Long appSystemId, Long envId) {
        if (!DeployAppAuthChecker.hasEnvPrivilege(appSystemId, envId)) {
            ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (appSystemCiEntity == null) {
                throw new CiEntityNotFoundException(appSystemId);
            }
            CiEntityVo envCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
            if (envCiEntity == null) {
                throw new CiEntityNotFoundException(envId);
            }
            throw new DeployAppEnvAuthException(appSystemCiEntity, envCiEntity);
        }
    }
}
