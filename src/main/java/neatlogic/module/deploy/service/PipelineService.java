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

import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineSearchVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;

import java.util.List;

public interface PipelineService {

    List<PipelineVo> searchPipeline(PipelineSearchVo searchVo);

    List<PipelineJobTemplateVo> searchPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo);

     void deleteDependency(DeployAppConfigVo deployAppConfigVo);

    /**
     * 找出修改部分配置信息
     * @param fullConfig 前端传过来的全量配置信息
     * @param parentConfig 如果当前层是环境层，parentConfig表示的是模块层修改部分配置信息；如果当前层是模块层，parentConfig应该为null。
     * @return
     */
    DeployPipelineConfigVo getModifiedPartConfig(DeployPipelineConfigVo fullConfig, DeployPipelineConfigVo parentConfig);

    /**
     * 保存应用流水线
     * @param deployAppConfigVo
     */
    void saveDeployAppPipeline(DeployAppConfigVo deployAppConfigVo);
}
