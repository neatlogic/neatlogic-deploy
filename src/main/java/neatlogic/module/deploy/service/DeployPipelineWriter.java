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

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopPhaseNameRepeatException;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.PipelineService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在领域模块内维护配置写入及业务校验，供管理页面与接入流程复用。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DeployPipelineWriter {
    @Resource private DeployAppConfigMapper deployAppConfigMapper;
    @Resource private DeployAppAuthorityService deployAppAuthorityService;
    @Resource private PipelineService pipelineService;

    /** 保存流水线及其引用关系，调用方与原页面共享权限校验和写入规则。 */
    public Object savePipeline(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        if (paramObj.getLong("envId") != null) {
            deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        }

        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        Long appSystemId = deployAppConfigVo.getAppSystemId();
        DeployAppConfigVo oldAppSystemAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId));
        // 通知等配置可能已创建应用根记录，未保存流水线时仍按首次保存处理。
        if (oldAppSystemAppConfigVo == null || oldAppSystemAppConfigVo.getConfig() == null) {
            // 在首次保存时需要重新生成阶段id和操作id
            regeneratePhaseIdAndOperationId(deployAppConfigVo);
            //只有新增流水线时才会配置执行器组（runner组）
            List<DeployAppModuleRunnerGroupVo> runnerGroupVoList = deployAppConfigVo.getConfig().getModuleRunnerGroupList();
            if (CollectionUtils.isNotEmpty(runnerGroupVoList)) {
                deployAppConfigMapper.insertAppModuleRunnerGroupList(runnerGroupVoList);
            }
        }

        if (oldAppSystemAppConfigVo != null && oldAppSystemAppConfigVo.getConfig() != null) {
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
}
