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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopPhaseNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
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
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.PipelineService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveDeployAppPipelineApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    PipelineService pipelineService;

    @Override
    public String getName() {
        return "nmdaa.savedeployapppipelineapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Description(desc = "nmdaa.savedeployapppipelineapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        if (paramObj.getLong("envId") != null) {
            deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        }

        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        Long appSystemId = deployAppConfigVo.getAppSystemId();
//        Long appModuleId = deployAppConfigVo.getAppModuleId();
//        Long envId = deployAppConfigVo.getEnvId();
        DeployAppConfigVo oldAppSystemAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId));
        if (oldAppSystemAppConfigVo == null) {
            // 在首次保存时需要重新生成阶段id和操作id
            regeneratePhaseIdAndOperationId(deployAppConfigVo);
            //只有新增流水线时才会配置执行器组（runner组）
            List<DeployAppModuleRunnerGroupVo> runnerGroupVoList = deployAppConfigVo.getConfig().getModuleRunnerGroupList();
            if (CollectionUtils.isNotEmpty(runnerGroupVoList)) {
                deployAppConfigMapper.insertAppModuleRunnerGroupList(runnerGroupVoList);
            }
        }

        if (oldAppSystemAppConfigVo != null) {
            // 补充作业参数列表数据
            List<AutoexecParamVo> autoexecParamList = oldAppSystemAppConfigVo.getConfig().getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(autoexecParamList)) {
                DeployPipelineConfigVo newConfigVo = deployAppConfigVo.getConfig();
                newConfigVo.setRuntimeParamList(autoexecParamList);
                deployAppConfigVo.setConfig(newConfigVo);
            }
        }
        setPhaseGroupId(deployAppConfigVo);
        pipelineService.saveDeployAppPipeline(deployAppConfigVo);
//        String configStr = deployAppConfigVo.getConfigStr();
//        IAutoexecCombopCrossoverService autoexecCombopCrossoverService = CrossoverServiceFactory.getApi(IAutoexecCombopCrossoverService.class);
//        autoexecCombopCrossoverService.verifyAutoexecCombopConfig(deployAppConfigVo.getConfig().getAutoexecCombopConfigVo(), false);
//        deployAppConfigVo.setConfigStr(configStr);
//        deployAppConfigVo.setFcu(UserContext.get().getUserUuid());
//        deployAppConfigVo.setLcu(UserContext.get().getUserUuid());
//        if (appModuleId == 0L && envId == 0L) {
//            // 应用层
//            if (oldAppSystemAppConfigVo != null) {
//                if (Objects.equals(oldAppSystemAppConfigVo.getConfigStr(), deployAppConfigVo.getConfigStr())) {
//                    return null;
//                } else {
//                    pipelineService.deleteDependency(oldAppSystemAppConfigVo);
//                    deployAppConfigVo.setId(oldAppSystemAppConfigVo.getId());
//                    deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
//                    saveDependency(deployAppConfigVo);
//                }
//            } else {
//                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
//                saveDependency(deployAppConfigVo);
//            }
//        } else if (envId == 0L) {
//            // 模块层
//            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId));
//            // 找出修改部分配置
//            DeployPipelineConfigVo modifiedPartConfig = pipelineService.getModifiedPartConfig(deployAppConfigVo.getConfig(), null);
//            if (modifiedPartConfig == null) {
//                if (oldAppModuleAppConfigVo != null) {
//                    pipelineService.deleteDependency(oldAppModuleAppConfigVo);
//                    deployAppConfigMapper.deleteAppModuleAppConfig(appSystemId, appModuleId);
//                }
//                return null;
//            }
//            deployAppConfigVo.setConfig(modifiedPartConfig);
//            if (oldAppModuleAppConfigVo != null) {
//                pipelineService.deleteDependency(oldAppModuleAppConfigVo);
//                deployAppConfigVo.setId(oldAppModuleAppConfigVo.getId());
//                deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
//                saveModifiedPartConfigDependency(deployAppConfigVo);
//            } else {
//                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
//                saveModifiedPartConfigDependency(deployAppConfigVo);
//            }
//        } else {
//            // 环境层
//            DeployPipelineConfigVo appModuleAppConfigConfig = null;
//            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId));
//            if (oldAppModuleAppConfigVo != null) {
//                appModuleAppConfigConfig = oldAppModuleAppConfigVo.getConfig();
//            }
//
//            DeployAppConfigVo oldAppEnvAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId, envId));
//            // 找出修改部分配置
//            DeployPipelineConfigVo modifiedPartConfig = pipelineService.getModifiedPartConfig(deployAppConfigVo.getConfig(), appModuleAppConfigConfig);
//            if (modifiedPartConfig == null) {
//                if (oldAppEnvAppConfigVo != null) {
//                    pipelineService.deleteDependency(oldAppEnvAppConfigVo);
//                    deployAppConfigMapper.deleteAppEnvAppConfig(appSystemId, appModuleId, envId);
//                }
//                return null;
//            }
//            deployAppConfigVo.setConfig(modifiedPartConfig);
//            if (oldAppEnvAppConfigVo != null) {
//                pipelineService.deleteDependency(oldAppEnvAppConfigVo);
//                deployAppConfigVo.setId(oldAppEnvAppConfigVo.getId());
//                deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
//                saveModifiedPartConfigDependency(deployAppConfigVo);
//            } else {
//                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
//                saveModifiedPartConfigDependency(deployAppConfigVo);
//            }
//        }
        deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigVo);
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

//    /**
//     * 保存重载部分阶段中操作工具对预置参数集和全局参数的引用关系、流水线对场景的引用关系
//     *
//     * @param deployAppConfigVo
//     */
//    private void saveModifiedPartConfigDependency(DeployAppConfigVo deployAppConfigVo) {
//        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
//        if (config == null) {
//            return;
//        }
//
//        Long appSystemId = deployAppConfigVo.getAppSystemId();
//        Long moduleId = deployAppConfigVo.getAppModuleId();
//        Long envId = deployAppConfigVo.getEnvId();
//
//        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
//        if (CollectionUtils.isEmpty(combopPhaseList)) {
//            return;
//        }
//
//        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
//            if (combopPhaseVo == null) {
//                continue;
//            }
//            if (moduleId != null) {
//                //如果是模块层或环境层，没有重载，就不用保存依赖关系
//                Integer override = combopPhaseVo.getOverride();
//                if (Objects.equals(override, 0)) {
//                    continue;
//                }
//            }
//            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
//            if (phaseConfig == null) {
//                continue;
//            }
//            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
//            if (CollectionUtils.isEmpty(phaseOperationList)) {
//                continue;
//            }
//            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
//                if (phaseOperationVo == null) {
//                    continue;
//                }
//                saveDependency(combopPhaseVo, phaseOperationVo, appSystemId, moduleId, envId);
//                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
//                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
//                if (CollectionUtils.isNotEmpty(ifList)) {
//                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
//                        if (operationVo == null) {
//                            continue;
//                        }
//                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
//                    }
//                }
//                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
//                if (CollectionUtils.isNotEmpty(elseList)) {
//                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
//                        if (operationVo == null) {
//                            continue;
//                        }
//                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
//                    }
//                }
//            }
//        }
//    }
//    /**
//     * 保存阶段中操作工具对预置参数集和全局参数的引用关系、流水线对场景的引用关系
//     *
//     * @param deployAppConfigVo
//     */
//    private void saveDependency(DeployAppConfigVo deployAppConfigVo) {
//        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
//        if (config == null) {
//            return;
//        }
//
//        Long appSystemId = deployAppConfigVo.getAppSystemId();
//        Long moduleId = deployAppConfigVo.getAppModuleId();
//        Long envId = deployAppConfigVo.getEnvId();
//
//        JSONObject dependencyConfig = new JSONObject();
//        dependencyConfig.put("appSystemId", appSystemId);
//        dependencyConfig.put("moduleId", moduleId);
//        dependencyConfig.put("envId", envId);
//
//        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
//        if (CollectionUtils.isNotEmpty(scenarioList)) {
//            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
//                dependencyConfig.put("scenarioId", scenarioVo.getScenarioId());
//                dependencyConfig.put("scenarioName", scenarioVo.getScenarioName());
//                DependencyManager.insert(AutoexecScenarioDeployPipelineDependencyHandler.class, scenarioVo.getScenarioId(), deployAppConfigVo.getId(), dependencyConfig);
//            }
//        }
//
//        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
//        if (CollectionUtils.isEmpty(combopPhaseList)) {
//            return;
//        }
//
//        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
//            if (combopPhaseVo == null) {
//                continue;
//            }
//            if (moduleId != null) {
//                //如果是模块层或环境层，没有重载，就不用保存依赖关系
//                Integer override = combopPhaseVo.getOverride();
//                if (Objects.equals(override, 0)) {
//                    continue;
//                }
//            }
//            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
//            if (phaseConfig == null) {
//                continue;
//            }
//            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
//            if (CollectionUtils.isEmpty(phaseOperationList)) {
//                continue;
//            }
//            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
//                if (phaseOperationVo == null) {
//                    continue;
//                }
//                saveDependency(combopPhaseVo, phaseOperationVo, appSystemId, moduleId, envId);
//                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
//                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
//                if (CollectionUtils.isNotEmpty(ifList)) {
//                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
//                        if (operationVo == null) {
//                            continue;
//                        }
//                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
//                    }
//                }
//                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
//                if (CollectionUtils.isNotEmpty(elseList)) {
//                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
//                        if (operationVo == null) {
//                            continue;
//                        }
//                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
//     *
//     * @param combopPhaseVo
//     * @param phaseOperationVo
//     * @param appSystemId
//     * @param moduleId
//     * @param envId
//     */
//    private void saveDependency(AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo, Long appSystemId, Long moduleId, Long envId) {
//        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
//        if (operationConfigVo == null) {
//            return;
//        }
//        Long profileId = operationConfigVo.getProfileId();
//        if (profileId != null) {
//            JSONObject dependencyConfig = new JSONObject();
//            dependencyConfig.put("appSystemId", appSystemId);
//            dependencyConfig.put("moduleId", moduleId);
//            dependencyConfig.put("envId", envId);
//            dependencyConfig.put("phaseId", combopPhaseVo.getId());
//            dependencyConfig.put("phaseName", combopPhaseVo.getName());
//            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
//        }
//        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
//        if (CollectionUtils.isNotEmpty(paramMappingList)) {
//            for (ParamMappingVo paramMappingVo : paramMappingList) {
//                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
//                    JSONObject dependencyConfig = new JSONObject();
//                    dependencyConfig.put("appSystemId", appSystemId);
//                    dependencyConfig.put("moduleId", moduleId);
//                    dependencyConfig.put("envId", envId);
//                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
//                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
//                    dependencyConfig.put("key", paramMappingVo.getKey());
//                    dependencyConfig.put("name", paramMappingVo.getName());
//                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
//                }
//            }
//        }
//        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
//        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
//            for (ParamMappingVo paramMappingVo : argumentMappingList) {
//                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
//                    JSONObject dependencyConfig = new JSONObject();
//                    dependencyConfig.put("appSystemId", appSystemId);
//                    dependencyConfig.put("moduleId", moduleId);
//                    dependencyConfig.put("envId", envId);
//                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
//                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
//                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
//                }
//            }
//        }
//    }
}
