/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package neatlogic.module.deploy.service;

import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.exception.DeployAppEnvAuthException;
import neatlogic.framework.deploy.exception.DeployAppOperationAuthException;
import neatlogic.framework.deploy.exception.DeployAppScenarioAuthException;
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
