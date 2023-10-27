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

package neatlogic.module.deploy.util;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import neatlogic.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.dto.pipeline.PipelineGroupVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineLaneVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class DeployPipelineConfigManager {

    private static DeployAppConfigMapper deployAppConfigMapper;

    private static AutoexecTypeMapper autoexecTypeMapper;

    private static AutoexecToolMapper autoexecToolMapper;

    @Resource
    public void setDeployAppConfigMapper(DeployAppConfigMapper _deployAppConfigMapper) {
        deployAppConfigMapper = _deployAppConfigMapper;
    }

    @Resource
    public void setAutoexecTypeMapper(AutoexecTypeMapper _autoexecTypeMapper) {
        autoexecTypeMapper = _autoexecTypeMapper;
    }

    @Resource
    public void setAutoexecToolMapper(AutoexecToolMapper _autoexecToolMapper) {
        autoexecToolMapper = _autoexecToolMapper;
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
        // 标识是否删除禁用阶段
        private boolean isDeleteDisabledPhase;
        /**
         * 是否需要更新配置信息中场景名称、预置参数集名称、操作对应工具信息
         */
        private boolean isUpdateConfig = true;

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

        public Builder withDeleteDisabledPhase(boolean isDeleteDisabledPhase) {
            this.isDeleteDisabledPhase = isDeleteDisabledPhase;
            return this;
        }

        public Builder isHasBuildOrDeployTypeTool(boolean _isHasBuildOrDeployTypeTool) {
            this.isHasBuildOrDeployTypeTool = _isHasBuildOrDeployTypeTool;
            return this;
        }

        public Builder isUpdateConfig(boolean _isUpdateConfig) {
            this.isUpdateConfig = _isUpdateConfig;
            return this;
        }

        public DeployPipelineConfigVo getConfig() {
            System.out.println("appSystemId = " + appSystemId);
            System.out.println("appModuleId = " + appModuleId);
            System.out.println("envId = " + envId);
            System.out.println("isAppSystemDraft = " + isAppSystemDraft);
            System.out.println("isAppModuleDraft = " + isAppModuleDraft);
            System.out.println("isEnvDraft = " + isEnvDraft);
            System.out.println("profileIdList = " + JSONObject.toJSONString(profileIdList));
            DeployPipelineConfigVo deployPipelineConfig = getDeployPipelineConfig(appSystemId, appModuleId, envId, isAppSystemDraft, isAppModuleDraft, isEnvDraft, profileIdList);
            if (deployPipelineConfig == null) {
                return null;
            }
            if (isDeleteDisabledPhase) {
                // 删除禁用阶段
                List<DeployPipelinePhaseVo> deployPipelinePhaseList = deployPipelineConfig.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(deployPipelinePhaseList)) {
                    boolean hasRemove = false;
                    Iterator<DeployPipelinePhaseVo> iterator = deployPipelinePhaseList.iterator();
                    while(iterator.hasNext()) {
                        DeployPipelinePhaseVo deployPipelinePhaseVo = iterator.next();
                        if (Objects.equals(deployPipelinePhaseVo.getIsActive(), 0)) {
                            iterator.remove();
                            hasRemove = true;
                        }
                    }
                    if (hasRemove) {
                        Set<Long> groupIdSet = new HashSet<>();
                        for (DeployPipelinePhaseVo deployPipelinePhaseVo : deployPipelinePhaseList) {
                            groupIdSet.add(deployPipelinePhaseVo.getGroupId());
                        }
                        List<DeployPipelineGroupVo> deployPipelineGroupList = deployPipelineConfig.getCombopGroupList();
                        if (CollectionUtils.isNotEmpty(deployPipelineGroupList)) {
                            Iterator<DeployPipelineGroupVo> groupIterator = deployPipelineGroupList.iterator();
                            while(groupIterator.hasNext()) {
                                DeployPipelineGroupVo deployPipelineGroupVo = groupIterator.next();
                                if (!groupIdSet.contains(deployPipelineGroupVo.getId())) {
                                    groupIterator.remove();
                                }
                            }
                        }
                    }
                }
            }
            if (isUpdateConfig) {
                IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
                autoexecServiceCrossoverService.updateAutoexecCombopConfig(deployPipelineConfig.getAutoexecCombopConfigVo());
            }
            if (isHasBuildOrDeployTypeTool) {
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
//        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();
        if (CollectionUtils.isEmpty(scenarioList)) {
            return;
        }
        Set<Long> operationIdSet = new HashSet<>();
        Map<Long, List<Long>> scenarioOperationIdListMap = new HashMap<>();
        Long buildTypeId = autoexecTypeMapper.getTypeIdByName("BUILD");
        Long deployTypeId = autoexecTypeMapper.getTypeIdByName("DEPLOY");
        for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
            List<Long> operationIdList = new ArrayList<>();
            scenarioOperationIdListMap.put(scenarioVo.getScenarioId(), operationIdList);
            List<String> combopPhaseNameList = scenarioVo.getCombopPhaseNameList();
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelineConfigVo.getCombopPhaseList()) {
                if (!combopPhaseNameList.contains(pipelinePhaseVo.getName())) {
                    continue;
                }
                if (Objects.equals(pipelinePhaseVo.getIsActive(), 0)) {
                    continue;
                }
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = pipelinePhaseVo.getConfig().getPhaseOperationList();
                for (AutoexecCombopPhaseOperationVo operationVo : phaseOperationList) {
                    if (Objects.equals(ToolType.TOOL.getValue(), operationVo.getOperationType())) {
                        operationIdSet.add(operationVo.getOperationId());
                        operationIdList.add(operationVo.getOperationId());
                    }
                }
//                for (AutoexecCombopPhaseOperationVo operationVo : phaseOperationList) {
//                    if (Objects.equals(ToolType.TOOL.getValue(), operationVo.getOperationType())) {
//                        AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecServiceCrossoverService.getAutoexecOperationBaseVoByIdAndType(pipelinePhaseVo.getName(), operationVo, false);
//                        if (autoexecOperationBaseVo != null && Objects.equals(autoexecOperationBaseVo.getTypeName(), "BUILD")) {
//                            scenarioVo.setIsHasBuildTypeTool(1);
//                        }
//                        if (autoexecOperationBaseVo != null && Objects.equals(autoexecOperationBaseVo.getTypeName(), "DEPLOY")) {
//                            scenarioVo.setIsHasDeployTypeTool(1);
//                        }
//                    }
//                    if (scenarioVo.getIsHasBuildTypeTool() == 1 && scenarioVo.getIsHasDeployTypeTool() == 1) {
//                        break;
//                    }
//                }
//                if (scenarioVo.getIsHasBuildTypeTool() == 1 && scenarioVo.getIsHasDeployTypeTool() == 1) {
//                    break;
//                }
            }
        }
        if (CollectionUtils.isEmpty(operationIdSet)) {
            return;
        }
        List<Long> buildTypeToolIdList = new ArrayList<>();
        List<Long> deployTypeToolIdList = new ArrayList<>();
        List<Long> operationIdList = new ArrayList<>(operationIdSet);
        if (buildTypeId != null) {
            buildTypeToolIdList = autoexecToolMapper.getToolIdListByIdListAndTypeId(operationIdList, buildTypeId);

        }
        if (deployTypeId != null) {
            deployTypeToolIdList = autoexecToolMapper.getToolIdListByIdListAndTypeId(operationIdList, deployTypeId);
        }
        for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
            List<Long> scenarioOperationIdList = scenarioOperationIdListMap.get(scenarioVo.getScenarioId());
            if (CollectionUtils.isEmpty(scenarioOperationIdList)) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(ListUtils.retainAll(scenarioOperationIdList, buildTypeToolIdList))) {
                scenarioVo.setIsHasBuildTypeTool(1);
            }
            if (CollectionUtils.isNotEmpty(ListUtils.retainAll(scenarioOperationIdList, deployTypeToolIdList))) {
                scenarioVo.setIsHasDeployTypeTool(1);
            }
        }
    }

    /**
     * 获取流水线配置信息
     *
     * @param appSystemId      应用id
     * @param appModuleId      应用模块id
     * @param envId            环境id
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
     * 判断超级流水线中是否含有编译和发布工具的作业模版
     *
     * @param appSystemId 系统id
     * @param appModuleId 模块id
     * @param pipeline    超级流水线
     */
    public static void judgeHasBuildOrDeployTypeToolInPipeline(Long appSystemId, Long appModuleId, PipelineVo pipeline) {
        Map<Long, DeployPipelineConfigVo> envPipelineMap = new HashMap<>();
        out:
        if (CollectionUtils.isNotEmpty(pipeline.getLaneList())) {
            for (int i = 0; i < pipeline.getLaneList().size(); i++) {
                PipelineLaneVo pipelineLaneVo = pipeline.getLaneList().get(i);
                if (CollectionUtils.isNotEmpty(pipelineLaneVo.getGroupList())) {
                    for (int j = 0; j < pipelineLaneVo.getGroupList().size(); j++) {
                        PipelineGroupVo pipelineGroupVo = pipelineLaneVo.getGroupList().get(j);
                        if (CollectionUtils.isNotEmpty(pipelineGroupVo.getJobTemplateList())) {
                            for (int k = 0; k < pipelineGroupVo.getJobTemplateList().size(); k++) {
                                PipelineJobTemplateVo jobTemplateVo = pipelineGroupVo.getJobTemplateList().get(k);
                                if (appSystemId == null || Objects.equals(jobTemplateVo.getAppSystemId(), appSystemId)) {
                                    if (appModuleId == null || Objects.equals(jobTemplateVo.getAppSystemId(), appSystemId)) {
                                        setIsJobTemplateVoHasBuildDeployType(jobTemplateVo, envPipelineMap);
                                        if (jobTemplateVo.getIsHasBuildTypeTool() != 1) {
                                            pipeline.setIsHasBuildTypeTool(jobTemplateVo.getIsHasBuildTypeTool());
                                        }
                                        if (jobTemplateVo.getIsHasDeployTypeTool() != 1) {
                                            pipeline.setIsHasDeployTypeTool(jobTemplateVo.getIsHasDeployTypeTool());
                                        }
                                    }
                                }
                                if (pipeline.getIsHasBuildTypeTool() == 1 && pipeline.getIsHasDeployTypeTool() == 1) {
                                    break out;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置超级流水线是否含有build｜deploy 工具
     *
     * @param jobTemplateVo  流水线
     * @param envPipelineMap 出重环境流水线map
     */
    public static void setIsJobTemplateVoHasBuildDeployType(PipelineJobTemplateVo jobTemplateVo, Map<Long, DeployPipelineConfigVo> envPipelineMap) {
        if (jobTemplateVo.getIsHasBuildTypeTool() == 1 && jobTemplateVo.getIsHasDeployTypeTool() == 1) {
            return;
        }
        DeployPipelineConfigVo pipelineConfigVo = envPipelineMap.get(jobTemplateVo.getEnvId());
        if (pipelineConfigVo == null) {
            pipelineConfigVo = DeployPipelineConfigManager.init(jobTemplateVo.getAppSystemId())
                    .withAppModuleId(jobTemplateVo.getAppModuleId())
                    .withEnvId(jobTemplateVo.getEnvId())
                    .isHasBuildOrDeployTypeTool(true)
                    .isUpdateConfig(false)
                    .getConfig();
            if (pipelineConfigVo != null) {
                envPipelineMap.put(jobTemplateVo.getEnvId(), pipelineConfigVo);
            }
        }
        if (pipelineConfigVo != null) {
            List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();
            if (CollectionUtils.isNotEmpty(scenarioList)) {
                Optional<AutoexecCombopScenarioVo> first = scenarioList.stream().filter(o -> Objects.equals(o.getScenarioId(), jobTemplateVo.getScenarioId())).findFirst();
                if (first.isPresent()) {
                    if (Objects.equals(first.get().getIsHasBuildTypeTool(), 1)) {
                        jobTemplateVo.setIsHasBuildTypeTool(1);
                    }
                    if (Objects.equals(first.get().getIsHasDeployTypeTool(), 1)) {
                        jobTemplateVo.setIsHasDeployTypeTool(1);
                    }
                }
            }
        }
    }

    /**
     * 组装应用、模块、环境的流水线配置信息
     *
     * @param appConfig            应用层配置信息
     * @param moduleOverrideConfig 模块层配置信息
     * @param envOverrideConfig    环境层配置信息
     * @param targetLevel          目标层
     * @param profileIdList        预置参数集id列表
     * @return 目标层配置信息
     */
    private static DeployPipelineConfigVo mergeDeployPipelineConfig(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel, List<Long> profileIdList) {
        if (Objects.equals(targetLevel, "应用")) {

        } else if (Objects.equals(targetLevel, "模块")) {
            initPipelineAppConfig(appConfig);
            pipelinePhaseSetSource(appConfig.getCombopPhaseList(), "应用");
            if (moduleOverrideConfig != null) {
                pipelinePhaseSetSource(moduleOverrideConfig.getCombopPhaseList(), "模块");
                mergeDeployPipelineConfig(appConfig, moduleOverrideConfig);
            }
        } else if (Objects.equals(targetLevel, "环境")) {
            initPipelineAppConfig(appConfig);
            pipelinePhaseSetSource(appConfig.getCombopPhaseList(), "应用");
            if (moduleOverrideConfig != null) {
                pipelinePhaseSetSource(moduleOverrideConfig.getCombopPhaseList(), "模块");
                mergeDeployPipelineConfig(appConfig, moduleOverrideConfig);
            }
            pipelineConfigReSetOverrideAndParentIsActiveAndInheritFieldValue(appConfig);
            if (envOverrideConfig != null) {
                pipelinePhaseSetSource(envOverrideConfig.getCombopPhaseList(), "环境");
                mergeDeployPipelineConfig(appConfig, envOverrideConfig);
            }
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
                mergeDeployPipelineConfigProfileList(deployProfileList, overrideProfileList);
                if (Objects.equals(targetLevel, "应用")) {

                } else if (Objects.equals(targetLevel, "模块")) {
                    overrideProfileList = null;
                    if (moduleOverrideConfig != null) {
                        overrideProfileList = moduleOverrideConfig.getOverrideProfileList();
                    }
                    mergeDeployPipelineConfigProfileList(deployProfileList, overrideProfileList);
                } else if (Objects.equals(targetLevel, "环境")) {
                    if (moduleOverrideConfig != null) {
                        overrideProfileList = moduleOverrideConfig.getOverrideProfileList();
                        if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                            mergeDeployPipelineConfigProfileList(deployProfileList, overrideProfileList);
                        }
                    }
                    overrideProfileList = null;
                    if (envOverrideConfig != null) {
                        overrideProfileList = envOverrideConfig.getOverrideProfileList();
                    }
                    mergeDeployPipelineConfigProfileList(deployProfileList, overrideProfileList);
                }
                appConfig.setOverrideProfileList(deployProfileList);
            }
        }
        return appConfig;
    }

    /**
     * 重置配置信息中override、parentIsActive、inherit字段值
     * @param appConfig
     */
    private static void pipelineConfigReSetOverrideAndParentIsActiveAndInheritFieldValue(DeployPipelineConfigVo appConfig) {
        List<DeployPipelinePhaseVo> pipelinePhaseList = appConfig.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(pipelinePhaseList)) {
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelinePhaseList) {
                pipelinePhaseVo.setOverride(0);
                if (Objects.equals(pipelinePhaseVo.getIsActive(), 0)) {
                    pipelinePhaseVo.setParentIsActive(0);
                }
            }
        }
        List<DeployPipelineGroupVo> pipelineGroupList = appConfig.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(pipelineGroupList)) {
            for (DeployPipelineGroupVo pipelineGroupVo : pipelineGroupList) {
                pipelineGroupVo.setInherit(1);
            }
        }
        DeployPipelineExecuteConfigVo executeConfigVo = appConfig.getExecuteConfig();
        if (executeConfigVo != null) {
            executeConfigVo.setInherit(1);
        }
    }

    /**
     * 初始化配置信息中override、isActive、inherit字段值
     * @param appConfig
     */
    private static void initPipelineAppConfig(DeployPipelineConfigVo appConfig) {
        List<DeployPipelinePhaseVo> pipelinePhaseList = appConfig.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(pipelinePhaseList)) {
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelinePhaseList) {
                pipelinePhaseVo.setOverride(0);
                pipelinePhaseVo.setIsActive(1);
            }
        }
        List<DeployPipelineGroupVo> pipelineGroupList = appConfig.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(pipelineGroupList)) {
            for (DeployPipelineGroupVo pipelineGroupVo : pipelineGroupList) {
                pipelineGroupVo.setInherit(1);
            }
        }
        DeployPipelineExecuteConfigVo executeConfigVo = appConfig.getExecuteConfig();
        if (executeConfigVo != null) {
            executeConfigVo.setInherit(1);
        }
    }

    /**
     * 合并配置信息中预置参数集列表
     * @param deployProfileList
     * @param overrideProfileList
     * @return
     */
    private static List<DeployProfileVo> mergeDeployPipelineConfigProfileList(List<DeployProfileVo> deployProfileList, List<DeployProfileVo> overrideProfileList) {
        for (DeployProfileVo deployProfileVo : deployProfileList) {
            List<DeployProfileParamVo> deployProfileParamList = deployProfileVo.getParamList();
            for (DeployProfileParamVo deployProfileParamVo : deployProfileParamList) {
                deployProfileParamVo.setInherit(1);
            }
        }
        if (CollectionUtils.isNotEmpty(overrideProfileList)) {
            for (DeployProfileVo overrideProfileVo : overrideProfileList) {
                for (DeployProfileVo deployProfileVo : deployProfileList) {
                    if (Objects.equals(overrideProfileVo.getProfileId(), deployProfileVo.getProfileId())) {
                        List<DeployProfileParamVo> overrideProfileParamList = overrideProfileVo.getParamList();
                        List<DeployProfileParamVo> deployProfileParamList = deployProfileVo.getParamList();
                        for (DeployProfileParamVo overrideProfileParamVo : overrideProfileParamList) {
                            if (Objects.equals(overrideProfileParamVo.getInherit(), 1)) {
                                continue;
                            }
                            int index = 0;
                            for (DeployProfileParamVo deployProfileParamVo : deployProfileParamList) {
                                if (Objects.equals(overrideProfileParamVo.getId(), deployProfileParamVo.getId())) {
                                    break;
                                }
                                index++;
                            }
                            deployProfileParamList.remove(index);
                            deployProfileParamList.add(index, overrideProfileParamVo);
                        }
                    }
                }
            }
        }
        return deployProfileList;
    }

    /**
     * 合并配置信息中阶段、阶段组、执行账号
     * @param appConfig
     * @param overrideConfig
     * @return
     */
    private static DeployPipelineConfigVo mergeDeployPipelineConfig(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo overrideConfig) {

        List<Long> disabledPhaseIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(overrideConfig.getDisabledPhaseIdList())) {
            disabledPhaseIdList = new ArrayList<>(overrideConfig.getDisabledPhaseIdList());
        } else {
            // 这里是为了兼容旧数据
            List<DeployPipelinePhaseVo> overridePhaseList = overrideConfig.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(overridePhaseList)) {
                for (DeployPipelinePhaseVo overridePhaseVo : overridePhaseList) {
                    if (Objects.equals(overridePhaseVo.getIsActive(), 0)) {
                        disabledPhaseIdList.add(overridePhaseVo.getId());
                    }
                }
            }
        }
        List<DeployPipelinePhaseVo> overridePhaseList = overrideConfig.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(overridePhaseList)) {
            List<DeployPipelinePhaseVo> pipelinePhaseList = appConfig.getCombopPhaseList();
            for (DeployPipelinePhaseVo overridePhaseVo : overridePhaseList) {
                if (!Objects.equals(overridePhaseVo.getOverride(), 1)) {
                    continue;
                }
                int index = 0;
                for (DeployPipelinePhaseVo pipelinePhaseVo : pipelinePhaseList) {
                    if (Objects.equals(overridePhaseVo.getUuid(), pipelinePhaseVo.getUuid())) {
                        overridePhaseVo.setParentIsActive(pipelinePhaseVo.getParentIsActive());
                        if (Objects.equals(overridePhaseVo.getIsActive(), 0)) {
                            disabledPhaseIdList.remove(overridePhaseVo.getId());
                        }
                        break;
                    }
                    index++;
                }
                pipelinePhaseList.remove(index);
                pipelinePhaseList.add(index, overridePhaseVo);
            }
        }
        if (CollectionUtils.isNotEmpty(disabledPhaseIdList)) {
            List<DeployPipelinePhaseVo> pipelinePhaseList = appConfig.getCombopPhaseList();
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelinePhaseList) {
                if (disabledPhaseIdList.contains(pipelinePhaseVo.getId())) {
                    pipelinePhaseVo.setIsActive(0);
                }
            }
        }
        List<DeployPipelineGroupVo> overrideGroupList = overrideConfig.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(overrideGroupList)) {
            List<DeployPipelineGroupVo> pipelineGroupList = appConfig.getCombopGroupList();
            for (DeployPipelineGroupVo overrideGroupVo : overrideGroupList) {
                if (!Objects.equals(overrideGroupVo.getInherit(), 0)) {
                    continue;
                }
                int index = 0;
                for (DeployPipelineGroupVo pipelineGroupVo : pipelineGroupList) {
                    if (Objects.equals(overrideGroupVo.getUuid(), pipelineGroupVo.getUuid())) {
                        break;
                    }
                    index++;
                }
                pipelineGroupList.remove(index);
                pipelineGroupList.add(index, overrideGroupVo);
            }
        }
        DeployPipelineExecuteConfigVo executeConfigVo = overrideConfig.getExecuteConfig();
        if (executeConfigVo != null && Objects.equals(executeConfigVo.getInherit(), 0)) {
            appConfig.setExecuteConfig(executeConfigVo);
        }
        return appConfig;
    }

    /**
     * 设置阶段中source字段值
     * @param pipelinePhaseList
     * @param source
     */
    private static void pipelinePhaseSetSource(List<DeployPipelinePhaseVo> pipelinePhaseList, String source) {
        if (CollectionUtils.isNotEmpty(pipelinePhaseList)) {
            for (DeployPipelinePhaseVo pipelinePhaseVo : pipelinePhaseList) {
                pipelinePhaseVo.setSource(source);
            }
        }
    }

}
