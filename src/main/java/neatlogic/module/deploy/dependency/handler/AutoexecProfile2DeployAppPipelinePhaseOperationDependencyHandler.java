/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.dependency.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelinePhaseVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import neatlogic.framework.util.$;
/**
 * 发布应用流水线阶段操作引用预置参数集处理器
 */
@Component
public class AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler extends DefaultDependencyHandlerBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long appSystemId = config.getLong("appSystemId");
        Long moduleId = config.getLong("moduleId");
        Long envId = config.getLong("envId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId, moduleId, envId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            return null;
        }
        DeployPipelineConfigVo pipelineConfigVo = deployAppConfigVo.getConfig();
        if (pipelineConfigVo == null) {
            return null;
        }
        List<DeployPipelinePhaseVo> combopPhaseList = pipelineConfigVo.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return null;
        }
        Long phaseId = config.getLong("phaseId");
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
            if (!Objects.equals(combopPhaseVo.getId(), phaseId)) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
            if (phaseConfigVo == null) {
                return null;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                return null;
            }
            Long id = Long.valueOf(dependencyVo.getTo());
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                if (!Objects.equals(phaseOperationVo.getId(), id)) {
                    continue;
                }
                AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
                if (operationConfigVo == null) {
                    return null;
                }
                if (!Objects.equals(operationConfigVo.getProfileId().toString(), dependencyVo.getFrom())) {
                    return null;
                }
                String operationName = phaseOperationVo.getOperationName();
                String phaseName = combopPhaseVo.getName();
                List<String> pathList = new ArrayList<>();
                pathList.add($.t("nmddh.autoexecprofile2deployapppipelinephaseoperationdependencyhandler.runtime.label"));
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                if (appSystemId != null && appSystemId != 0) {
                    CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
                    if (ciEntityVo != null) {
                        pathList.add(ciEntityVo.getName());
                    }
                }
                if (moduleId != null && moduleId != 0) {
                    CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(moduleId);
                    if (ciEntityVo != null) {
                        pathList.add(ciEntityVo.getName());
                    }
                }
                if (envId != null && envId != 0) {
                    CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
                    if (ciEntityVo != null) {
                        pathList.add(ciEntityVo.getName());
                    }
                }
                pathList.add(phaseName);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("/");
                stringBuilder.append(TenantContext.get().getTenantUuid());
                stringBuilder.append("/deploy.html#/application-config-pipeline-detail?appSystemId=${DATA.appSystemId}");
                JSONObject dependencyInfoConfig = new JSONObject();
                dependencyInfoConfig.put("appSystemId", appSystemId);
                if (moduleId != null && moduleId != 0L) {
                    dependencyInfoConfig.put("moduleId", moduleId);
                    stringBuilder.append("&appModuleId=${DATA.moduleId}");
                    if (envId != null && envId != 0L) {
                        dependencyInfoConfig.put("envId", envId);
                        stringBuilder.append("&envId=${DATA.envId}");
                    }
                }

                String urlFormat = stringBuilder.toString();
                return new DependencyInfoVo(id, dependencyInfoConfig, operationName, pathList, urlFormat, this.getGroupName());
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.PROFILE;
    }
}
