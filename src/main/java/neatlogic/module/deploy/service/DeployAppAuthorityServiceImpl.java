/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.service;

import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.exception.DeployAppEnvAuthException;
import neatlogic.framework.deploy.exception.DeployAppOperationAuthException;
import neatlogic.framework.deploy.exception.DeployAppScenarioAuthException;
import neatlogic.module.deploy.auth.core.DeployAppAuthChecker;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DeployAppAuthorityServiceImpl implements DeployAppAuthorityService {

    @Override
    public void checkOperationAuth(Long appSystemId, DeployAppConfigAction action) {
        Set<String> authList = DeployAppAuthChecker.builder(appSystemId).addOperationAction(action.getValue()).check();

        if (!authList.contains(action.getValue())) {
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
        Set<String> authList = DeployAppAuthChecker.builder(appSystemId).addEnvAction(envId).check();
        if (!authList.contains(envId.toString())) {
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

    @Override
    public void checkScenarioAuth(Long appSystemId, Long scenarioId) {
        Set<String> authList = DeployAppAuthChecker.builder(appSystemId).addScenarioAction(scenarioId).check();
        if (!authList.contains(scenarioId.toString())) {
            ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (appSystemCiEntity == null) {
                throw new CiEntityNotFoundException(appSystemId);
            }
            IAutoexecScenarioCrossoverMapper iAutoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
            AutoexecScenarioVo scenarioVo = iAutoexecScenarioCrossoverMapper.getScenarioById(scenarioId);
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(scenarioId);
            }
            throw new DeployAppScenarioAuthException(appSystemCiEntity, scenarioVo);
        }
    }

    @Override
    public void checkEnvAuthList(Long appSystemId, List<Long> envIdList) {

        Set<String> authSet = DeployAppAuthChecker.builder(appSystemId).addEnvActionList(envIdList).check();
        for (Long envId : envIdList) {
            if (!authSet.contains(envId.toString())) {
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

}
