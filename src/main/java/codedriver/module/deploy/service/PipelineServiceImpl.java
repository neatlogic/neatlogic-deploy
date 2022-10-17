/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.PIPELINE_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineSearchVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.module.deploy.auth.core.DeployAppAuthChecker;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecScenarioDeployPipelineDependencyHandler;
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

        String dependencyToString = "systemId&" + deployAppConfigVo.getAppSystemId().toString() + (deployAppConfigVo.getAppModuleId() != null ? "moduleId&" + deployAppConfigVo.getAppModuleId() : "") + (deployAppConfigVo.getEnvId() != null ? "envId&" + deployAppConfigVo.getEnvId() : "");
        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            DependencyManager.delete(AutoexecScenarioDeployPipelineDependencyHandler.class, dependencyToString);
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
