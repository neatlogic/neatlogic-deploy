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
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployImportExportHandlerType;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.PipelineService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@Component
public class AppPipelineImportExportHandler extends ImportExportHandlerBase {

    private Logger logger = LoggerFactory.getLogger(AppPipelineImportExportHandler.class);

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private PipelineService pipelineService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public ImportExportHandlerType getType() {
        return DeployImportExportHandlerType.APP_PIPELINE;
    }

    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(importExportVo.getName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(importExportVo.getName());
        }
        try {
            deployAppAuthorityService.checkOperationAuth(appSystem.getId(), DeployAppConfigAction.EDIT);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
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
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(importExportVo.getName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(importExportVo.getName());
        }
        return appSystem.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(importExportVo.getName());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(importExportVo.getName());
        }
        JSONObject data = importExportVo.getData();
        DeployAppPipelineExportVo deployAppPipelineExportVo = data.toJavaObject(DeployAppPipelineExportVo.class);
        List<DeployAppConfigVo> appConfigList = deployAppPipelineExportVo.getAppConfigList();
        Iterator<DeployAppConfigVo> iterator = appConfigList.iterator();
        while (iterator.hasNext()) {
            DeployAppConfigVo appConfigVo = iterator.next();
            String appSystemAbbrName = appConfigVo.getAppSystemAbbrName();
            appSystem = resourceCrossoverMapper.getAppSystemByName(appSystemAbbrName);
            if (appSystem != null) {
                appConfigVo.setAppSystemId(appSystem.getId());
            }
            boolean flag = dependencyHandle(IMPORT, appConfigVo, null, null, primaryChangeList);
            if (flag) {
                pipelineService.saveDeployAppPipeline(appConfigVo);
            }
        }
        return null;
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
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
            boolean flag = dependencyHandle(EXPORT, appConfigVo, dependencyList, zipOutputStream, null);
            if (!flag) {
                iterator.remove();
            }
        }
        DeployAppPipelineExportVo deployAppPipelineExportVo = new DeployAppPipelineExportVo();
        deployAppPipelineExportVo.setAppSystemId(appSystem.getId());
        deployAppPipelineExportVo.setAppSystemName(appSystem.getName());
        deployAppPipelineExportVo.setAppSystemAbbrName(appSystem.getAbbrName());
        deployAppPipelineExportVo.setAppConfigList(appConfigList);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, appSystem.getAbbrName());
        importExportVo.setDataWithObject(deployAppPipelineExportVo);
        return importExportVo;
    }

    /**
     * 导出处理，先导出依赖组件
     * 导入处理，更新依赖组件的唯一标识
     * @param action
     * @param appConfigVo
     * @param dependencyList
     * @param zipOutputStream
     * @param primaryChangeList
     */
    private boolean dependencyHandle(
            String action,
            DeployAppConfigVo appConfigVo,
            List<ImportExportBaseInfoVo> dependencyList,
            ZipOutputStream zipOutputStream,
            List<ImportExportPrimaryChangeVo> primaryChangeList
    ) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        DeployPipelineConfigVo config = appConfigVo.getConfig();
        Long appModuleId = appConfigVo.getAppModuleId();
        String appModuleAbbrName = appConfigVo.getAppModuleAbbrName();
        Long envId = appConfigVo.getEnvId();
        String envName = appConfigVo.getEnvName();
        if (appModuleId == 0 && envId == 0) {
            // 应用层
            // 阶段
            List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                    dependencyHandleDeployPipelinePhase(action, deployPipelinePhaseVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 阶段组
            List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
            if (CollectionUtils.isNotEmpty(combopGroupList)) {
                for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                    dependencyHandleDeployPipelineGroup(action, deployPipelineGroupVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 执行账户
            if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList, zipOutputStream);
                }
            }
            // 预置参数集
            List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
            if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                for (DeployProfileVo deployProfileVo : overrideProfileList) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList, zipOutputStream);
                    }
                }
            }
            // 场景
            List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
            for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : scenarioList) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, autoexecCombopScenarioVo.getScenarioId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        autoexecCombopScenarioVo.setScenarioId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, autoexecCombopScenarioVo.getScenarioId(), dependencyList, zipOutputStream);
                }
            }
        } else if (envId == 0) {
            // 模块层
            if (StringUtils.isBlank(appModuleAbbrName)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The module is simply empty");
                }
            }
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(appModuleAbbrName);
            if (appModule == null) {
                return false;
            }
            appConfigVo.setAppModuleId(appModule.getId());
            appConfigVo.setAppModuleName(appModule.getName());
            // 阶段
            List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                    dependencyHandleDeployPipelinePhase(action, deployPipelinePhaseVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 阶段组
            List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
            if (CollectionUtils.isNotEmpty(combopGroupList)) {
                for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                    dependencyHandleDeployPipelineGroup(action, deployPipelineGroupVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 执行账户
            if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList, zipOutputStream);
                }
            }
            // 预置参数集
            List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
            if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                for (DeployProfileVo deployProfileVo : overrideProfileList) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList, zipOutputStream);
                    }
                }
            }
        } else {
            // 环境层
            if (StringUtils.isBlank(appModuleAbbrName)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The module is simply empty");
                }
            }
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(appModuleAbbrName);
            if (appModule == null) {
                return false;
            }
            appConfigVo.setAppModuleId(appModule.getId());
            appConfigVo.setAppModuleName(appModule.getName());

            if (StringUtils.isBlank(envName)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("The environment name is empty");
                }
            }
            ResourceVo env = resourceCrossoverMapper.getAppEnvByName(envName);
            if (env == null) {
                return false;
            }
            appConfigVo.setEnvId(env.getId());
            // 阶段
            List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                    dependencyHandleDeployPipelinePhase(action, deployPipelinePhaseVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 阶段组
            List<DeployPipelineGroupVo> combopGroupList = config.getCombopGroupList();
            if (CollectionUtils.isNotEmpty(combopGroupList)) {
                for (DeployPipelineGroupVo deployPipelineGroupVo : combopGroupList) {
                    dependencyHandleDeployPipelineGroup(action, deployPipelineGroupVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 执行账户
            if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList, zipOutputStream);
                }

            }
            // 预置参数集
            List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
            if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                for (DeployProfileVo deployProfileVo : overrideProfileList) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            deployProfileVo.setProfileId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, deployProfileVo.getProfileId(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
        return true;
    }

    /**
     * 导入时处理阶段中的配置信息，例如，protocolId发生变化时替换成新的值
     * 导出时处理阶段中配置信息
     * @param action
     * @param deployPipelinePhaseVo
     * @param dependencyList
     * @param zipOutputStream
     * @param primaryChangeList
     */
    private void dependencyHandleDeployPipelinePhase(
            String action,
            DeployPipelinePhaseVo deployPipelinePhaseVo,
            List<ImportExportBaseInfoVo> dependencyList,
            ZipOutputStream zipOutputStream,
            List<ImportExportPrimaryChangeVo> primaryChangeList
    ) {
        AutoexecCombopPhaseConfigVo config = deployPipelinePhaseVo.getConfig();
        if (config == null) {
            return;
        }
        // 执行目标配置
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig != null) {
            if (Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
                if (executeConfig.getProtocolId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            executeConfig.setProtocolId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT)  {
                        doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = config.getPhaseOperationList();
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
            dependencyHandleAutoexecCombopPhaseOperation(action, phaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
        }
    }

    /**
     * 导入时处理阶段组中的配置信息，例如，protocolId发生变化时替换成新的值
     * 导出时处理阶段组中配置信息
     * @param action
     * @param deployPipelineGroupVo
     * @param dependencyList
     * @param zipOutputStream
     * @param primaryChangeList
     */
    private void dependencyHandleDeployPipelineGroup(
            String action,
            DeployPipelineGroupVo deployPipelineGroupVo,
            List<ImportExportBaseInfoVo> dependencyList,
            ZipOutputStream zipOutputStream,
            List<ImportExportPrimaryChangeVo> primaryChangeList
    ) {
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
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    executeConfig.setProtocolId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList, zipOutputStream);
            }
        }
    }

    /**
     * 导入时处理阶段操作中的配置信息，例如，profileId发生变化时替换成新的值
     * 导出时处理阶段操作中配置信息
     * @param action
     * @param phaseOperationVo
     * @param dependencyList
     * @param zipOutputStream
     * @param primaryChangeList
     */
    private void dependencyHandleAutoexecCombopPhaseOperation(
            String action,
            AutoexecCombopPhaseOperationVo phaseOperationVo,
            List<ImportExportBaseInfoVo> dependencyList,
            ZipOutputStream zipOutputStream,
            List<ImportExportPrimaryChangeVo> primaryChangeList
    ) {
        if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.SCRIPT.getValue())) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    phaseOperationVo.setOperationId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), dependencyList, zipOutputStream);
            }
        } else if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.TOOL.getValue())) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    phaseOperationVo.setOperationId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), dependencyList, zipOutputStream);
            }
        }
        AutoexecCombopPhaseOperationConfigVo phaseOperationConfig = phaseOperationVo.getConfig();
        if (phaseOperationConfig != null) {
            if (phaseOperationConfig.getProfileId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        phaseOperationConfig.setProfileId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), dependencyList, zipOutputStream);
                }
            }
            if (action == EXPORT) {
                List<ParamMappingVo> paramMappingList = phaseOperationConfig.getParamMappingList();
                if (CollectionUtils.isNotEmpty(paramMappingList)) {
                    for (ParamMappingVo paramMappingVo : paramMappingList) {
                        if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList, zipOutputStream);
                        }
                    }
                }
                List<ParamMappingVo> argumentMappingList = phaseOperationConfig.getArgumentMappingList();
                if (CollectionUtils.isNotEmpty(argumentMappingList)) {
                    for (ParamMappingVo paramMappingVo : argumentMappingList) {
                        if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList, zipOutputStream);
                        }
                    }
                }
            }
            List<AutoexecCombopPhaseOperationVo> ifList = phaseOperationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo ifPhaseOperationVo : ifList) {
                    dependencyHandleAutoexecCombopPhaseOperation(action, ifPhaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = phaseOperationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo elsePhaseOperationVo : elseList) {
                    dependencyHandleAutoexecCombopPhaseOperation(action, elsePhaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> operations = phaseOperationConfig.getOperations();
            if (CollectionUtils.isNotEmpty(operations)) {
                for (AutoexecCombopPhaseOperationVo loopOperation : operations) {
                    dependencyHandleAutoexecCombopPhaseOperation(action, loopOperation, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
        }
    }
}
