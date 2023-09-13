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

package neatlogic.module.deploy.importexport.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.constvalue.AutoexecJobGroupPolicy;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.framework.deploy.constvalue.DeployImportExportHandlerType;
import neatlogic.module.deploy.service.PipelineService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Component
public class AppPipelineImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private PipelineService pipelineService;

    @Override
    public ImportExportHandlerType getType() {
        return DeployImportExportHandlerType.APP_PIPELINE;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(importExportBaseInfoVo.getName());
        if (appSystem == null) {
            return false;
        }
        List<DeployAppConfigVo> appConfigList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystem.getId());
        if (CollectionUtils.isNotEmpty(appConfigList)) {
            return true;
        }
        return false;
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        JSONObject data = importExportVo.getData();
        DeployAppPipelineExportVo deployAppPipelineExportVo = data.toJavaObject(DeployAppPipelineExportVo.class);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(deployAppPipelineExportVo.getAppSystemAbbrName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(deployAppPipelineExportVo.getAppSystemAbbrName());
        }
        List<DeployAppConfigVo> appConfigList = deployAppPipelineExportVo.getAppConfigList();
        Iterator<DeployAppConfigVo> iterator = appConfigList.iterator();
        while (iterator.hasNext()) {
            DeployAppConfigVo appConfigVo = iterator.next();
            DeployPipelineConfigVo config = appConfigVo.getConfig();
            Long appModuleId = appConfigVo.getAppModuleId();
            Long envId = appConfigVo.getEnvId();
            if (appModuleId == 0 && envId == 0) {
                // 应用层
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        importHandlerDeployPipelinePhase(deployPipelinePhaseVo, primaryChangeList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        importHandlerDeployPipelineGroup(deployPipelineGroupVo, primaryChangeList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    }
                }
                // 场景
                List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
                for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : scenarioList) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, autoexecCombopScenarioVo.getScenarioId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        autoexecCombopScenarioVo.setScenarioId((Long) newPrimaryKey);
                    }
                }
            } else if (envId == 0) {
                // 模块层
                ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
                if (appModule == null) {
                    iterator.remove();
                    continue;
                }
                appConfigVo.setAppModuleName(appModule.getName());
                appConfigVo.setAppModuleAbbrName(appModule.getAbbrName());
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        importHandlerDeployPipelinePhase(deployPipelinePhaseVo, primaryChangeList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        importHandlerDeployPipelineGroup(deployPipelineGroupVo, primaryChangeList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    }
                }
            } else {
                // 环境层
                ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
                if (appModule == null) {
                    iterator.remove();
                }
                appConfigVo.setAppModuleName(appModule.getName());
                appConfigVo.setAppModuleAbbrName(appModule.getAbbrName());

                ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId);
                if (env == null) {
                    iterator.remove();
                    continue;
                }
                appConfigVo.setEnvName(appConfigVo.getEnvName());
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        importHandlerDeployPipelinePhase(deployPipelinePhaseVo, primaryChangeList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        importHandlerDeployPipelineGroup(deployPipelineGroupVo, primaryChangeList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    }
                }
            }
            pipelineService.saveDeployAppPipeline(appConfigVo);
        }
        return null;
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportVo> dependencyList) {
        Long appSystemId = (Long) primaryKey;
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        List<DeployAppConfigVo> appConfigList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isEmpty(appConfigList)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        Iterator<DeployAppConfigVo> iterator = appConfigList.iterator();
        while (iterator.hasNext()) {
            DeployAppConfigVo appConfigVo = iterator.next();
            appConfigVo.setAppSystemName(appSystem.getName());
            appConfigVo.setAppSystemAbbrName(appSystem.getAbbrName());
            DeployPipelineConfigVo config = appConfigVo.getConfig();
            Long appModuleId = appConfigVo.getAppModuleId();
            Long envId = appConfigVo.getEnvId();
            if (appModuleId == 0 && envId == 0) {
                // 应用层
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        exportHandlerDeployPipelinePhase(deployPipelinePhaseVo, dependencyList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        exportHandlerDeployPipelineGroup(deployPipelineGroupVo, dependencyList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList);
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList);
                    }
                }
                // 场景
                List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
                for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : scenarioList) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, autoexecCombopScenarioVo.getScenarioId(), dependencyList);
                }
            } else if (envId == 0) {
                // 模块层
                ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
                if (appModule == null) {
                    iterator.remove();
                    continue;
                }
                appConfigVo.setAppModuleName(appModule.getName());
                appConfigVo.setAppModuleAbbrName(appModule.getAbbrName());
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        exportHandlerDeployPipelinePhase(deployPipelinePhaseVo, dependencyList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        exportHandlerDeployPipelineGroup(deployPipelineGroupVo, dependencyList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList);
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList);
                    }
                }
            } else {
                // 环境层
                ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
                if (appModule == null) {
                    iterator.remove();
                }
                appConfigVo.setAppModuleName(appModule.getName());
                appConfigVo.setAppModuleAbbrName(appModule.getAbbrName());

                ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId);
                if (env == null) {
                    iterator.remove();
                    continue;
                }
                appConfigVo.setEnvName(appConfigVo.getEnvName());
                // 阶段
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        exportHandlerDeployPipelinePhase(deployPipelinePhaseVo, dependencyList);
                    }
                }
                // 阶段组
                List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                        exportHandlerDeployPipelineGroup(deployPipelineGroupVo, dependencyList);
                    }
                }
                // 执行账户
                if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList);
                }
                // 预置参数集
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                    for (DeployProfileVo deployProfileVo : overrideProfileList) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList);
                    }
                }
            }
        }
        DeployAppPipelineExportVo deployAppPipelineExportVo = new DeployAppPipelineExportVo();
        deployAppPipelineExportVo.setAppSystemId(appSystem.getId());
        deployAppPipelineExportVo.setAppSystemName(appSystem.getName());
        deployAppPipelineExportVo.setAppSystemAbbrName(appSystem.getAbbrName());
        deployAppPipelineExportVo.setAppConfigList(appConfigList);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, appSystem.getName());
        importExportVo.setDataWithObject(deployAppPipelineExportVo);
        return importExportVo;
    }

    private void exportHandlerDeployPipelinePhase(DeployPipelinePhaseVo deployPipelinePhaseVo, List<ImportExportVo> dependencyList) {
        AutoexecCombopPhaseConfigVo config = deployPipelinePhaseVo.getConfig();
        if (config == null) {
            return;
        }
        // 执行目标配置
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig != null) {
            if (Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
                if (executeConfig.getProtocolId() != null) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList);
                }
            }
        }
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = config.getPhaseOperationList();
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
            exportHandlerAutoexecCombopPhaseOperation(phaseOperationVo, dependencyList);
        }
    }

    private void exportHandlerAutoexecCombopPhaseOperation(AutoexecCombopPhaseOperationVo phaseOperationVo, List<ImportExportVo> dependencyList) {
        if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.SCRIPT.getValue())) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), dependencyList);
        } else if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.TOOL.getValue())) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), dependencyList);
        }
        AutoexecCombopPhaseOperationConfigVo phaseOperationConfig = phaseOperationVo.getConfig();
        if (phaseOperationConfig == null) {
            if (phaseOperationConfig.getProfileId() != null) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), dependencyList);
            }
            List<ParamMappingVo> paramMappingList = phaseOperationConfig.getParamMappingList();
            if (CollectionUtils.isNotEmpty(paramMappingList)) {
                for (ParamMappingVo paramMappingVo : paramMappingList) {
                    if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList);
                    }
                }
            }
            List<ParamMappingVo> argumentMappingList = phaseOperationConfig.getArgumentMappingList();
            if (CollectionUtils.isNotEmpty(argumentMappingList)) {
                for (ParamMappingVo paramMappingVo : argumentMappingList) {
                    if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList);
                    }
                }
            }
            List<AutoexecCombopPhaseOperationVo> ifList = phaseOperationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo ifPhaseOperationVo : ifList) {
                    exportHandlerAutoexecCombopPhaseOperation(ifPhaseOperationVo, dependencyList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = phaseOperationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo elsePhaseOperationVo : elseList) {
                    exportHandlerAutoexecCombopPhaseOperation(elsePhaseOperationVo, dependencyList);
                }
            }
        }
    }

    private void exportHandlerDeployPipelineGroup(DeployPipelineGroupVo deployPipelineGroupVo, List<ImportExportVo> dependencyList) {
        if (Objects.equals(deployPipelineGroupVo.getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName())) {
            return;
        }
        AutoexecCombopGroupConfigVo config = deployPipelineGroupVo.getConfig();
        if (config == null) {
            return;
        }
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig == null) {
            return;
        }
        if (!Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
            return;
        }
        if (executeConfig.getProtocolId() != null) {
            doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList);
        }
    }

    private void importHandlerDeployPipelinePhase(DeployPipelinePhaseVo deployPipelinePhaseVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecCombopPhaseConfigVo config = deployPipelinePhaseVo.getConfig();
        if (config == null) {
            return;
        }
        // 执行目标配置
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig != null) {
            if (Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
                if (executeConfig.getProtocolId() != null) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        executeConfig.setProtocolId((Long) newPrimaryKey);
                    }
                }
            }
        }
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = config.getPhaseOperationList();
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
            importHandlerAutoexecCombopPhaseOperation(phaseOperationVo, primaryChangeList);
        }
    }

    private void importHandlerAutoexecCombopPhaseOperation(AutoexecCombopPhaseOperationVo phaseOperationVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.SCRIPT.getValue())) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), primaryChangeList);
            if (newPrimaryKey != null) {
                phaseOperationVo.setOperationId((Long) newPrimaryKey);
            }
        } else if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.TOOL.getValue())) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), primaryChangeList);
            if (newPrimaryKey != null) {
                phaseOperationVo.setOperationId((Long) newPrimaryKey);
            }
        }
        AutoexecCombopPhaseOperationConfigVo phaseOperationConfig = phaseOperationVo.getConfig();
        if (phaseOperationConfig == null) {
            if (phaseOperationConfig.getProfileId() != null) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    phaseOperationConfig.setProfileId((Long) newPrimaryKey);
                }
            }
            List<AutoexecCombopPhaseOperationVo> ifList = phaseOperationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo ifPhaseOperationVo : ifList) {
                    importHandlerAutoexecCombopPhaseOperation(ifPhaseOperationVo, primaryChangeList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = phaseOperationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo elsePhaseOperationVo : elseList) {
                    importHandlerAutoexecCombopPhaseOperation(elsePhaseOperationVo, primaryChangeList);
                }
            }
        }
    }

    private void importHandlerDeployPipelineGroup(DeployPipelineGroupVo deployPipelineGroupVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (Objects.equals(deployPipelineGroupVo.getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName())) {
            return;
        }
        AutoexecCombopGroupConfigVo config = deployPipelineGroupVo.getConfig();
        if (config == null) {
            return;
        }
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig == null) {
            return;
        }
        if (!Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
            return;
        }
        if (executeConfig.getProtocolId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
            if (newPrimaryKey != null) {
                executeConfig.setProtocolId((Long) newPrimaryKey);
            }
        }
    }
}
