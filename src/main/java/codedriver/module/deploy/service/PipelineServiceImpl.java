/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class PipelineServiceImpl implements PipelineService {
    private final static Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);
    @Resource
    PipelineMapper pipelineMapper;


    @Override
    public List<PipelineVo> searchPipeline(PipelineVo pipelineVo) {
        int rowNum = pipelineMapper.searchPipelineCount(pipelineVo);
        pipelineVo.setRowNum(rowNum);
        return pipelineMapper.searchPipeline(pipelineVo);
    }

    @Override
    public List<PipelineJobTemplateVo> searchPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo) {
        int rowNum = pipelineMapper.searchJobTemplateCount(pipelineJobTemplateVo);
        pipelineJobTemplateVo.setRowNum(rowNum);
        return pipelineMapper.searchJobTemplate(pipelineJobTemplateVo);
    }

    @Override
    public void deleteDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
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
