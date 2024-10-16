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
     * 重置工具为null
     *
     * @param operationList 工具列表
     */
    private void resetOperationNull(List<AutoexecCombopPhaseOperationVo> operationList) {
        if (CollectionUtils.isNotEmpty(operationList)) {
            for (AutoexecCombopPhaseOperationVo operationVo : operationList) {
                if (operationVo == null) {
                    continue;
                }
                operationVo.setId(null);
                AutoexecCombopPhaseOperationConfigVo operationConfig = operationVo.getConfig();
                resetOperationNull(operationConfig.getIfList());
                resetOperationNull(operationConfig.getElseList());
                resetOperationNull(operationConfig.getOperations());
            }
        }
    }

    /**
     * 重新生成操作id
     *
     * @param combopPhaseVo 组合工具阶段
     */
    private void regenerateOperationId(AutoexecCombopPhaseVo combopPhaseVo) {
        AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
        if (phaseConfig == null) {
            return;
        }
        resetOperationNull(phaseConfig.getPhaseOperationList());
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
