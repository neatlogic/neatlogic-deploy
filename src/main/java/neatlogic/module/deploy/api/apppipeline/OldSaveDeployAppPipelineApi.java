/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.apppipeline;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopPhaseNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecScenarioDeployPipelineDependencyHandler;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.PipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class OldSaveDeployAppPipelineApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    PipelineService pipelineService;

    @Override
    public String getName() {
        return "保存应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/save/old";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线配置信息")
    })
    @Description(desc = "保存应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        if (paramObj.getLong("envId") != null) {
            deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        }

        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        DeployPipelineConfigVo newConfigVo = deployAppConfigVo.getConfig();
        DeployAppConfigVo oldDeployAppConfigVo = deployAppConfigMapper.getAppConfigVo(deployAppConfigVo);
        if (oldDeployAppConfigVo != null) {
            DeployPipelineConfigVo oldConfigVo = oldDeployAppConfigVo.getConfig();
            List<AutoexecParamVo> autoexecParamList = oldConfigVo.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(autoexecParamList)) {
                newConfigVo.setRuntimeParamList(autoexecParamList);
                deployAppConfigVo.setConfig(newConfigVo);
            }
        } else {
            //只有新增流水线时才会配置执行器组（runner组）
            List<DeployAppModuleRunnerGroupVo> runnerGroupVoList = deployAppConfigVo.getConfig().getModuleRunnerGroupList();
            if (CollectionUtils.isNotEmpty(runnerGroupVoList)) {
//                deployAppConfigMapper.insertAppModuleRunnerGroupList(runnerGroupVoList);
            }
        }
        DeployPipelineExecuteConfigVo executeConfigVo = newConfigVo.getExecuteConfig();
//        if (StringUtils.isBlank(executeConfigVo.getExecuteUser())) {
//            throw new ParamNotExistsException("执行用户(config.executeUser)");
//        }
//        if (executeConfigVo.getProtocolId() == null) {
//            throw new ParamNotExistsException("连接协议(config.protocolId)");
//        }
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();
        if (envId != null && envId != 0) {
            //环境层，需要对重载过的阶段的操作重新生成id
            regenerateOperationId(deployAppConfigVo);
        } else if (moduleId != null && moduleId != 0) {
            //模块层，需要对重载过的阶段的操作重新生成id
            regenerateOperationId(deployAppConfigVo);
        } else {
            //应用层，在首次保存时需要重新生成阶段id和操作id
            if (oldDeployAppConfigVo == null) {
                regeneratePhaseIdAndOperationId(deployAppConfigVo);
            }
        }
        setPhaseGroupId(deployAppConfigVo);
        String configStr = deployAppConfigVo.getConfigStr();
        IAutoexecCombopCrossoverService autoexecCombopCrossoverService = CrossoverServiceFactory.getApi(IAutoexecCombopCrossoverService.class);
        autoexecCombopCrossoverService.verifyAutoexecCombopConfig(deployAppConfigVo.getConfig().getAutoexecCombopConfigVo(), false);
        deployAppConfigVo.setConfigStr(configStr);
        if (oldDeployAppConfigVo != null) {
            pipelineService.deleteDependency(oldDeployAppConfigVo);
            deployAppConfigVo.setLcu(UserContext.get().getUserUuid());
            deployAppConfigVo.setId(oldDeployAppConfigVo.getId());
            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
            saveDependency(deployAppConfigVo);
            deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigVo);
        } else {
            deployAppConfigVo.setFcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
            saveDependency(deployAppConfigVo);
        }
        return null;
    }

    /**
     * 设置阶段的组id
     *
     * @param deployAppConfigVo
     */
    private void setPhaseGroupId(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        Map<String, DeployPipelineGroupVo> groupMap = new HashMap<>();
        List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (DeployPipelineGroupVo groupVo : combopGroupList) {
                groupMap.put(groupVo.getUuid(), groupVo);
            }
        }
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            AutoexecCombopGroupVo autoexecCombopGroupVo = groupMap.get(combopPhaseVo.getGroupUuid());
            if (autoexecCombopGroupVo != null) {
                combopPhaseVo.setGroupId(autoexecCombopGroupVo.getId());
            }
        }
    }

    /**
     * 重新生成阶段id和操作id
     *
     * @param deployAppConfigVo
     */
    private void regeneratePhaseIdAndOperationId(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        List<String> nameList = new ArrayList<>();
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            String name = combopPhaseVo.getName();
            if (nameList.contains(name)) {
                throw new AutoexecCombopPhaseNameRepeatException(name);
            }
            nameList.add(name);
            combopPhaseVo.setId(null);
            regenerateOperationId(combopPhaseVo);
        }
    }

    /**
     * 重新生成操作id
     *
     * @param deployAppConfigVo
     */
    private void regenerateOperationId(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (!Objects.equals(combopPhaseVo.getOverride(), 1)) {
                continue;
            }
            regenerateOperationId(combopPhaseVo);
        }
    }

    /**
     * 重新生成操作id
     *
     * @param combopPhaseVo
     */
    private void regenerateOperationId(AutoexecCombopPhaseVo combopPhaseVo) {
        AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
        if (phaseConfig == null) {
            return;
        }
        List<AutoexecCombopPhaseOperationVo> operationList = phaseConfig.getPhaseOperationList();
        if (CollectionUtils.isEmpty(operationList)) {
            return;
        }
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : operationList) {
            if (phaseOperationVo == null) {
                continue;
            }
            phaseOperationVo.setId(null);
            AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
            List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                    if (operationVo == null) {
                        continue;
                    }
                    operationVo.setId(null);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                    if (operationVo == null) {
                        continue;
                    }
                    operationVo.setId(null);
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系、流水线对场景的引用关系
     *
     * @param deployAppConfigVo
     */
    private void saveDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }

        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();

        JSONObject dependencyConfig = new JSONObject();
        dependencyConfig.put("appSystemId", appSystemId);
        dependencyConfig.put("moduleId", moduleId);
        dependencyConfig.put("envId", envId);

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
                dependencyConfig.put("scenarioId", scenarioVo.getScenarioId());
                dependencyConfig.put("scenarioName", scenarioVo.getScenarioName());
                DependencyManager.insert(AutoexecScenarioDeployPipelineDependencyHandler.class, scenarioVo.getScenarioId(), deployAppConfigVo.getId(), dependencyConfig);
            }
        }

        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }

        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                saveDependency(combopPhaseVo, phaseOperationVo, appSystemId, moduleId, envId);
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
                    }
                }
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param combopPhaseVo
     * @param phaseOperationVo
     * @param appSystemId
     * @param moduleId
     * @param envId
     */
    private void saveDependency(AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo, Long appSystemId, Long moduleId, Long envId) {
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
        if (operationConfigVo == null) {
            return;
        }
        Long profileId = operationConfigVo.getProfileId();
        if (profileId != null) {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("appSystemId", appSystemId);
            dependencyConfig.put("moduleId", moduleId);
            dependencyConfig.put("envId", envId);
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("key", paramMappingVo.getKey());
                    dependencyConfig.put("name", paramMappingVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
    }
}
