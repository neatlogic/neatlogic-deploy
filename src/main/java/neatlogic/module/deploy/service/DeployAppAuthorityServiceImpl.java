/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.deploy.service;

import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
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
            IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
            if (env == null) {
                throw new AppEnvNotFoundException(envId);
            }
            throw new DeployAppEnvAuthException(appSystemCiEntity, env);
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
                IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
                ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
                if (env == null) {
                    throw new AppEnvNotFoundException(envId);
                }
                throw new DeployAppEnvAuthException(appSystemCiEntity, env);
            }
        }
    }

}
