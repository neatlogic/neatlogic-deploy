/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.util;

import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class DeployPipelineConfigManager {

    private static DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    public void setDeployAppConfigMapper(DeployAppConfigMapper _deployAppConfigMapper) {
        deployAppConfigMapper = _deployAppConfigMapper;
    }

    public static Builder init(Long appSystemId) {
        return new Builder(appSystemId);
    }

    public static class Builder {
        private final Long appSystemId;
        private Long appModuleId = 0L;
        private Long envId = 0L;
        private boolean isAppSystemDraft;
        private boolean isAppModuleDraft;
        private boolean isEnvDraft;
        private boolean isHasBuildOrDeployTypeTool;
        private List<Long> profileIdList;

        public Builder(Long appSystemId) {
            this.appSystemId = appSystemId;
        }

        public Builder withAppModuleId(Long appModuleId) {
            if (appModuleId != null) {
                this.appModuleId = appModuleId;
            }
            return this;
        }

        public Builder withEnvId(Long envId) {
            if (envId != null) {
                this.envId = envId;
            }
            return this;
        }
        public Builder withAppSystemDraft(boolean isAppSystemDraft) {
            this.isAppSystemDraft = isAppSystemDraft;
            return this;
        }
        public Builder withAppModuleDraft(boolean isAppModuleDraft) {
            this.isAppModuleDraft = isAppModuleDraft;
            return this;
        }

        public Builder withEnvDraft(boolean isEnvDraft) {
            this.isEnvDraft = isEnvDraft;
            return this;
        }

        public Builder withProfileIdList(List<Long> profileIdList) {
            this.profileIdList = profileIdList;
            return this;
        }

        public Builder isHasBuildOrDeployTypeTool(boolean _isHasBuildOrDeployTypeTool) {
            this.isHasBuildOrDeployTypeTool = _isHasBuildOrDeployTypeTool;
            return this;
        }

        public DeployPipelineConfigVo getConfig() {
            DeployPipelineConfigVo deployPipelineConfig = getDeployPipelineConfig(appSystemId, appModuleId, envId, isAppSystemDraft, isAppModuleDraft, isEnvDraft, profileIdList);
            if (deployPipelineConfig != null && isHasBuildOrDeployTypeTool) {
                setIsHasBuildOrDeployTypeTool(deployPipelineConfig);
            }
            return deployPipelineConfig;
        }

    }

    /**
     * 设置DeployPipelinePhaseVo中isHasBuildTypeTool和isHasDeployTypeTool字段值
     *
     * @param pipelineConfigVo 配置
     */
    private static void setIsHasBuildOrDeployTypeTool(DeployPipelineConfigVo pipelineConfigVo) {
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();
        if (CollectionUtils.isEmpty(scenarioList)) {
            return;
        }
        for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
            List<String> combopPhaseNameList = scenarioVo.getCombopPhaseNameList();
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelineConfigVo.getCombopPhaseList()) {
                if (!combopPhaseNameList.contains(pipelinePhaseVo.getName())) {
                    continue;
                }
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = pipelinePhaseVo.getConfig().getPhaseOperationList();
                for (AutoexecCombopPhaseOperationVo operationVo : phaseOperationList) {
                    if (Objects.equals(ToolType.TOOL.getValue(), operationVo.getOperationType())) {
                        AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecServiceCrossoverService.getAutoexecOperationBaseVoByIdAndType(pipelinePhaseVo.getName(), operationVo, false);
                        if (autoexecOperationBaseVo != null && Objects.equals(autoexecOperationBaseVo.getTypeName(), "BUILD")) {
                            scenarioVo.setIsHasBuildTypeTool(1);
                        }
                        if (autoexecOperationBaseVo != null && Objects.equals(autoexecOperationBaseVo.getTypeName(), "DEPLOY")) {
                            scenarioVo.setIsHasDeployTypeTool(1);
                        }
                    }
                    if (scenarioVo.getIsHasBuildTypeTool() == 1 && scenarioVo.getIsHasDeployTypeTool() == 1) {
                        break;
                    }
                }
                if (scenarioVo.getIsHasBuildTypeTool() == 1 && scenarioVo.getIsHasDeployTypeTool() == 1) {
                    break;
                }
            }
        }

    }

    /**
     * 获取流水线配置信息
     *
     * @param appSystemId 应用id
     * @param appModuleId 应用模块id
     * @param envId       环境id
     * @param isAppSystemDraft 是否取应用层配置草稿
     * @param isAppModuleDraft 是否取模块层配置草稿
     * @param isEnvDraft       是否取环境层配置草稿
     * @return 配置
     */
    private static DeployPipelineConfigVo getDeployPipelineConfig(Long appSystemId, Long appModuleId, Long envId, boolean isAppSystemDraft, boolean isAppModuleDraft, boolean isEnvDraft, List<Long> profileIdList) {
        String targetLevel;
        DeployPipelineConfigVo appConfig;
        DeployPipelineConfigVo moduleOverrideConfig = null;
        DeployPipelineConfigVo envOverrideConfig = null;
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId, appModuleId, envId);
        if (appModuleId == 0L && envId == 0L) {
            targetLevel = "应用";
            //查询应用层流水线配置信息
            appConfig = getDeployPipelineConfigVo(searchVo, isAppSystemDraft);
            if (appConfig == null) {
                if (isAppSystemDraft) {
                    return null;
                } else {
                    appConfig = new DeployPipelineConfigVo();
                }
            }
        } else if (appModuleId == 0L) {
            // 如果是访问环境层配置信息，moduleId不能为空
            throw new ParamNotExistsException("moduleId");
        } else if (envId == 0L) {
            targetLevel = "模块";
            //查询应用层配置信息
            appConfig = getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId), false);
            if (appConfig == null) {
                appConfig = new DeployPipelineConfigVo();
            }
            moduleOverrideConfig = getDeployPipelineConfigVo(searchVo, isAppModuleDraft);
            if (isAppModuleDraft && moduleOverrideConfig == null) {
                return null;
            }
        } else {
            targetLevel = "环境";
            //查询应用层配置信息
            appConfig = getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId), false);
            if (appConfig == null) {
                appConfig = new DeployPipelineConfigVo();
            }
            moduleOverrideConfig = getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId, appModuleId), false);
            envOverrideConfig = getDeployPipelineConfigVo(searchVo, isEnvDraft);
            if (isEnvDraft && envOverrideConfig == null) {
                return null;
            }
        }
        DeployPipelineConfigVo deployPipelineConfigVo = mergeDeployPipelineConfig(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel, profileIdList);
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        autoexecServiceCrossoverService.updateAutoexecCombopConfig(deployPipelineConfigVo.getAutoexecCombopConfigVo());
        return deployPipelineConfigVo;
    }

    private static DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo searchVo, boolean isDraft) {
        if (isDraft) {
            DeployAppConfigVo deployAppConfigDraftVo = deployAppConfigMapper.getAppConfigDraft(searchVo);
            if (deployAppConfigDraftVo != null) {
                return deployAppConfigDraftVo.getConfig();
            }
        } else {
            DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
            if (deployAppConfigVo != null) {
                return deployAppConfigVo.getConfig();
            }
        }
        return null;
    }

    /**
     * 组装应用、模块、环境的流水线配置信息
     *
     * @param appConfig 应用层配置信息
     * @param moduleOverrideConfig 模块层配置信息
     * @param envOverrideConfig 环境层配置信息
     * @param targetLevel 目标层
     * @param profileIdList 预置参数集id列表
     * @return 目标层配置信息
     */
    private static DeployPipelineConfigVo mergeDeployPipelineConfig(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel, List<Long> profileIdList) {
        overrideProfileParamSetSource(appConfig.getOverrideProfileList(), "应用");
        if (moduleOverrideConfig == null && envOverrideConfig == null) {
            if (!Objects.equals(targetLevel, "应用")) {
                overridePhase(appConfig.getCombopPhaseList());
                appConfig.getExecuteConfig().setInherit(1);
            }
        } else if (moduleOverrideConfig != null && envOverrideConfig == null) {
            overrideExecuteConfig(appConfig.getExecuteConfig(), moduleOverrideConfig.getExecuteConfig());
            if (Objects.equals(targetLevel, "环境")) {
                overridePhase(appConfig.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList(), "模块");
                appConfig.getExecuteConfig().setInherit(1);
            } else {
                overridePhase(appConfig.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList());
            }
            overridePhaseGroup(appConfig.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
            overrideProfile(appConfig.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
        } else if (moduleOverrideConfig == null) {
            overrideExecuteConfig(appConfig.getExecuteConfig(), envOverrideConfig.getExecuteConfig());
            overridePhase(appConfig.getCombopPhaseList(), envOverrideConfig.getCombopPhaseList());
            overridePhaseGroup(appConfig.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "环境");
            overrideProfile(appConfig.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
        } else {
            overrideExecuteConfig(appConfig.getExecuteConfig(), moduleOverrideConfig.getExecuteConfig());
            List<DeployPipelinePhaseVo> appSystemCombopPhaseList = appConfig.getCombopPhaseList();
            overridePhase(appSystemCombopPhaseList, moduleOverrideConfig.getCombopPhaseList(), "模块");
            overridePhaseGroup(appConfig.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
            overrideProfile(appConfig.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());

            overrideExecuteConfig(appConfig.getExecuteConfig(), envOverrideConfig.getExecuteConfig());
            overridePhase(appSystemCombopPhaseList, envOverrideConfig.getCombopPhaseList());
            overridePhaseGroup(appConfig.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "环境");
            overrideProfile(appConfig.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
        }

        if (CollectionUtils.isEmpty(profileIdList)) {
            profileIdList = new ArrayList<>(getProfileIdSet(appConfig));
        }
        if (CollectionUtils.isNotEmpty(profileIdList)) {
            IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
            List<AutoexecProfileVo> profileList = autoexecProfileCrossoverService.getProfileVoListByIdList(profileIdList);
            if (CollectionUtils.isNotEmpty(profileList)) {
                List<DeployProfileVo> deployProfileList = getDeployProfileList(profileList);
                List<DeployProfileVo> overrideProfileList = appConfig.getOverrideProfileList();
                finalOverrideProfile(deployProfileList, overrideProfileList);
                appConfig.setOverrideProfileList(deployProfileList);
            }
        }
        overrideProfileParamSetInherit(appConfig.getOverrideProfileList(), targetLevel);
        return appConfig;
    }

    /**
     * 覆盖阶段列表配置信息
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     */
    private static void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, null);
    }

    /**
     * 覆盖阶段列表配置信息
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList  模块层或环境层阶段列表数据
     */
    private static void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, overrideCombopPhaseList, null);
    }

    /**
     * 覆盖阶段列表配置信息
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList  模块层或环境层阶段列表数据
     * @param source 来源层名称
     */
    private static void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList, String source) {
        if (CollectionUtils.isEmpty(appSystemCombopPhaseList)) {
            return;
        }
        for (DeployPipelinePhaseVo appSystemCombopPhaseVo : appSystemCombopPhaseList) {
            if (StringUtils.isBlank(appSystemCombopPhaseVo.getSource())) {
                appSystemCombopPhaseVo.setSource("应用");
            }
            if (appSystemCombopPhaseVo.getOverride() == null) {
                appSystemCombopPhaseVo.setOverride(0);
            }
            if (appSystemCombopPhaseVo.getIsActive() == null) {
                appSystemCombopPhaseVo.setIsActive(1);
            }
            if (CollectionUtils.isEmpty(overrideCombopPhaseList)) {
                continue;
            }
            for (DeployPipelinePhaseVo overrideCombopPhaseVo : overrideCombopPhaseList) {
                if (!Objects.equals(appSystemCombopPhaseVo.getName(), overrideCombopPhaseVo.getName())) {
                    continue;
                }
                if (Objects.equals(overrideCombopPhaseVo.getOverride(), 1)) {
                    if (StringUtils.isNotBlank(source)) {
                        appSystemCombopPhaseVo.setSource(source);
                        appSystemCombopPhaseVo.setOverride(0);
                    } else {
                        appSystemCombopPhaseVo.setOverride(1);
                    }
                    appSystemCombopPhaseVo.setConfig(overrideCombopPhaseVo.getConfig());
                } else {
                    AutoexecCombopPhaseConfigVo appSystemPhaseConfigVo = appSystemCombopPhaseVo.getConfig();
                    AutoexecCombopPhaseConfigVo overridePhaseConfigVo = overrideCombopPhaseVo.getConfig();
                    AutoexecCombopExecuteConfigVo executeConfigVo = overridePhaseConfigVo.getExecuteConfig();
                    if (executeConfigVo != null) {
                        appSystemPhaseConfigVo.setExecuteConfig(executeConfigVo);
                    }
                }
                Integer parentIsActive = appSystemCombopPhaseVo.getIsActive();
                appSystemCombopPhaseVo.setParentIsActive(parentIsActive);
                if (parentIsActive == 0) {
                    appSystemCombopPhaseVo.setIsActive(0);
                } else {
                    appSystemCombopPhaseVo.setIsActive(overrideCombopPhaseVo.getIsActive());
                }
                break;
            }
        }
    }

    /**
     * 覆盖阶段组列表配置信息
     *
     * @param appSystemCombopGroupList 应用层阶段组列表数据
     * @param overrideCombopGroupList  模块层或环境层阶段组列表数据
     */
    private static void overridePhaseGroup(List<AutoexecCombopGroupVo> appSystemCombopGroupList, List<AutoexecCombopGroupVo> overrideCombopGroupList) {
        if (CollectionUtils.isNotEmpty(appSystemCombopGroupList) && CollectionUtils.isNotEmpty(overrideCombopGroupList)) {
            for (AutoexecCombopGroupVo appSystemCombopGroup : appSystemCombopGroupList) {
                for (AutoexecCombopGroupVo overrideCombopGroup : overrideCombopGroupList) {
                    if (Objects.equals(appSystemCombopGroup.getUuid(), overrideCombopGroup.getUuid())) {
                        appSystemCombopGroup.setPolicy(overrideCombopGroup.getPolicy());
                        appSystemCombopGroup.setConfig(overrideCombopGroup.getConfigStr());
                    }
                }
            }
        }
    }

    /**
     * 覆盖预置参数集列表数据
     *
     * @param appSystemDeployProfileList 应用层预置参数集列表数据
     * @param overrideDeployProfileList  模块层或环境层预置参数集列表数据
     */
    private static void overrideProfile(List<DeployProfileVo> appSystemDeployProfileList, List<DeployProfileVo> overrideDeployProfileList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        if (CollectionUtils.isEmpty(appSystemDeployProfileList)) {
            appSystemDeployProfileList.addAll(overrideDeployProfileList);
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            boolean flag = true;
            for (DeployProfileVo appSystemDeployProfile : appSystemDeployProfileList) {
                if (Objects.equals(appSystemDeployProfile.getProfileId(), overrideDeployProfile.getProfileId())) {
                    flag = false;
                    List<DeployProfileParamVo> appSystemDeployProfileParamList = appSystemDeployProfile.getParamList();
                    List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
                    overrideProfileParam(appSystemDeployProfileParamList, overrideDeployProfileParamList);
                    break;
                }
            }
            if (flag) {
                appSystemDeployProfileList.add(overrideDeployProfile);
            }
        }
    }

    /**
     * 覆盖预置参数集列表数据
     *
     * @param appSystemDeployProfileParamList 应用层预置参数集列表数据
     * @param overrideDeployProfileParamList  模块层或环境层预置参数集列表数据
     */
    private static void overrideProfileParam(List<DeployProfileParamVo> appSystemDeployProfileParamList, List<DeployProfileParamVo> overrideDeployProfileParamList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
            return;
        }
        if (CollectionUtils.isEmpty(appSystemDeployProfileParamList)) {
            appSystemDeployProfileParamList.addAll(overrideDeployProfileParamList);
            return;
        }
        for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
            boolean flag = true;
            for (DeployProfileParamVo appSystemDeployProfileParam : appSystemDeployProfileParamList) {
                if (Objects.equals(appSystemDeployProfileParam.getKey(), overrideDeployProfileParam.getKey())) {
                    flag = false;
                    if (overrideDeployProfileParam.getInherit() == 1) {
                        continue;
                    }
                    appSystemDeployProfileParam.setSource(overrideDeployProfileParam.getSource());
                    appSystemDeployProfileParam.setDefaultValue(overrideDeployProfileParam.getDefaultValue());
                    break;
                }
            }
            if (flag) {
                appSystemDeployProfileParamList.add(overrideDeployProfileParam);
            }
        }
    }

    /**
     * 设置预置参数的inherit字段值
     *
     * @param overrideDeployProfileList 预置参数集列表数据
     * @param targetLevel               目标层级
     */
    private static void overrideProfileParamSetInherit(List<DeployProfileVo> overrideDeployProfileList, String targetLevel) {
        if (CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
            if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
                continue;
            }
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (Objects.equals(overrideDeployProfileParam.getSource(), targetLevel)) {
                    overrideDeployProfileParam.setInherit(0);
                } else {
                    overrideDeployProfileParam.setInherit(1);
                }
            }
        }
    }

    /**
     * 设置预置参数的source字段值
     *
     * @param overrideDeployProfileList 预置参数集列表数据
     * @param source                    来源名
     */
    private static void overrideProfileParamSetSource(List<DeployProfileVo> overrideDeployProfileList, String source) {
        if (CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
            if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
                continue;
            }
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (Objects.equals(overrideDeployProfileParam.getInherit(), 0)) {
                    overrideDeployProfileParam.setSource(source);
                }
            }
        }
    }

    /**
     * 获取流水线的阶段列表中引用预置参数集列表的profileId列表
     *
     * @param config 流水线配置信息
     * @return 预置参数集Id列表
     */
    private static Set<Long> getProfileIdSet(DeployPipelineConfigVo config) {
        Set<Long> profileIdSet = new HashSet<>();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return profileIdSet;
        }
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
            if (phaseConfigVo == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
            if (CollectionUtils.isEmpty(combopPhaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo combopPhaseOperationVo : combopPhaseOperationList) {
                AutoexecCombopPhaseOperationConfigVo operationConfigVo = combopPhaseOperationVo.getConfig();
                if (operationConfigVo == null) {
                    continue;
                }
                Long profileId = operationConfigVo.getProfileId();
                if (profileId != null) {
                    profileIdSet.add(profileId);
                }
            }
        }
        return profileIdSet;
    }

    /**
     * 将AutoexecProfileVo列表转化成DeployProfileVo列表
     *
     * @param profileList 自动化预置参数集信息列表
     * @return 流水线预置参数集信息列表
     */
    private static List<DeployProfileVo> getDeployProfileList(List<AutoexecProfileVo> profileList) {
        List<DeployProfileVo> deployProfileList = new ArrayList<>();
        for (AutoexecProfileVo autoexecProfileVo : profileList) {
            DeployProfileVo deployProfileVo = new DeployProfileVo();
            deployProfileVo.setProfileId(autoexecProfileVo.getId());
            deployProfileVo.setProfileName(autoexecProfileVo.getName());
            List<AutoexecProfileParamVo> profileParamList = autoexecProfileVo.getProfileParamVoList();
            if (CollectionUtils.isNotEmpty(profileParamList)) {
                List<DeployProfileParamVo> deployProfileParamList = new ArrayList<>();
                for (AutoexecProfileParamVo autoexecProfileParamVo : profileParamList) {
                    DeployProfileParamVo deployProfileParamVo = new DeployProfileParamVo(autoexecProfileParamVo);
                    deployProfileParamVo.setInherit(1);
                    deployProfileParamVo.setSource("预置参数集");
                    deployProfileParamList.add(deployProfileParamVo);
                }
                deployProfileVo.setParamList(deployProfileParamList);
            }
            deployProfileList.add(deployProfileVo);
        }
        return deployProfileList;
    }

    /**
     * 最终覆盖预置参数集列表数据
     *
     * @param deployProfileList         原始的预置参数集列表
     * @param overrideDeployProfileList 流水线修改后的预置参数集列表
     */
    private static void finalOverrideProfile(List<DeployProfileVo> deployProfileList, List<DeployProfileVo> overrideDeployProfileList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo deployProfile : deployProfileList) {
            for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
                if (Objects.equals(deployProfile.getProfileId(), overrideDeployProfile.getProfileId())) {
                    List<DeployProfileParamVo> deployProfileParamList = deployProfile.getParamList();
                    List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
                    finalOverrideProfileParam(deployProfileParamList, overrideDeployProfileParamList);
                    break;
                }
            }
        }
    }

    /**
     * 最终覆盖预置参数集列表数据
     *
     * @param deployProfileParamList         原始的预置参数集列表
     * @param overrideDeployProfileParamList 流水线修改后的预置参数集列表
     */
    private static void finalOverrideProfileParam(List<DeployProfileParamVo> deployProfileParamList, List<DeployProfileParamVo> overrideDeployProfileParamList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
            return;
        }
        for (DeployProfileParamVo deployProfileParam : deployProfileParamList) {
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (overrideDeployProfileParam.getInherit() == 1) {
                    continue;
                }
                if (Objects.equals(deployProfileParam.getKey(), overrideDeployProfileParam.getKey())) {
                    deployProfileParam.setInherit(overrideDeployProfileParam.getInherit());
                    deployProfileParam.setSource(overrideDeployProfileParam.getSource());
                    deployProfileParam.setDefaultValue(overrideDeployProfileParam.getDefaultValue());
                    break;
                }
            }
        }
    }

    /**
     * 覆盖执行信息数据
     *
     * @param appSystemExecuteConfigVo 应用层执行信息数据
     * @param overrideExecuteConfigVo  模块层或环境层执行信息数据
     */
    private static void overrideExecuteConfig(DeployPipelineExecuteConfigVo appSystemExecuteConfigVo, DeployPipelineExecuteConfigVo overrideExecuteConfigVo) {
        Integer inherit = overrideExecuteConfigVo.getInherit();
        if (Objects.equals(inherit, 0)) {
            appSystemExecuteConfigVo.setInherit(inherit);
            appSystemExecuteConfigVo.setProtocolId(overrideExecuteConfigVo.getProtocolId());
            appSystemExecuteConfigVo.setExecuteUser(overrideExecuteConfigVo.getExecuteUser());
        } else {
            appSystemExecuteConfigVo.setInherit(1);
        }
    }

}
