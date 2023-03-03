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

package neatlogic.module.deploy.service;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelinePhaseVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineSearchVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.module.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecScenarioDeployPipelineDependencyHandler;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PipelineServiceImpl implements PipelineService {
    private final static Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);
    @Resource
    DeployPipelineMapper deployPipelineMapper;


    @Override
    public List<PipelineVo> searchPipeline(PipelineSearchVo searchVo) {
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<PipelineVo> pipelineList = deployPipelineMapper.getPipelineListByIdList(idList);
            for (PipelineVo pipeline : pipelineList) {
                if (pipeline.getAppSystemId() != null) {
                    AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(pipeline.getAppSystemId());
                    if (appSystemVo != null) {
                        pipeline.setAppSystemName(appSystemVo.getName());
                        pipeline.setAppSystemAbbrName(appSystemVo.getAbbrName());
                    }
                }
            }
            return pipelineList;
        }
        // 判断是否需要验证权限
        int isHasAllAuthority = 0;
        if (Objects.equals(searchVo.getNeedVerifyAuth(), 1)) {
            String type = searchVo.getType();
            if (StringUtils.isNotBlank(type)) {
                if (PipelineType.APPSYSTEM.getValue().equals(type)) {
                    Set<String> actionSet = DeployAppAuthChecker.builder(searchVo.getAppSystemId())
                            .addOperationAction(DeployAppConfigAction.PIPELINE.getValue())
                            .check();
                    if (actionSet.contains(DeployAppConfigAction.PIPELINE.getValue())) {
                        isHasAllAuthority = 1;
                    }
                } else if (PipelineType.GLOBAL.getValue().equals(type)) {
                    if (AuthActionChecker.check(PIPELINE_MODIFY.class)) {
                        isHasAllAuthority = 1;
                    }
                }
            }
        } else {
            // 不需要验证权限的话，就当拥有所有权限
            isHasAllAuthority = 1;
        }

        searchVo.setIsHasAllAuthority(isHasAllAuthority);
        searchVo.setAuthUuid(UserContext.get().getUserUuid());
        int rowNum = deployPipelineMapper.searchPipelineCount(searchVo);
        searchVo.setRowNum(rowNum);
        List<PipelineVo> pipelineList = deployPipelineMapper.searchPipeline(searchVo);
        Map<Long, AppSystemVo> appSystemMap = new HashMap<>();
        List<Long> appSystemIdList = pipelineList.stream().map(PipelineVo::getAppSystemId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            List<AppSystemVo> appSystemList = appSystemMapper.getAppSystemListByIdList(appSystemIdList);
            appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        for (PipelineVo pipeline : pipelineList) {
            if (pipeline.getAppSystemId() != null) {
                AppSystemVo appSystemVo = appSystemMap.get(pipeline.getAppSystemId());
                if (appSystemVo != null) {
                    pipeline.setAppSystemName(appSystemVo.getName());
                    pipeline.setAppSystemAbbrName(appSystemVo.getAbbrName());
                }
            }
        }
        return pipelineList;
    }

    @Override
    public List<PipelineJobTemplateVo> searchPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo) {
        int rowNum = deployPipelineMapper.searchJobTemplateCount(pipelineJobTemplateVo);
        pipelineJobTemplateVo.setRowNum(rowNum);
        return deployPipelineMapper.searchJobTemplate(pipelineJobTemplateVo);
    }

    @Override
    public void deleteDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            DependencyManager.delete(AutoexecScenarioDeployPipelineDependencyHandler.class, deployAppConfigVo.getId());
        }

        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        Long moduleId = deployAppConfigVo.getAppModuleId();
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
                DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, phaseOperationVo.getId());
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                    }
                }
            }
        }
    }
}
