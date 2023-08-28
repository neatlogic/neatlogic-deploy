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
     * 删除模块层或环境层重载部分配置的依赖关系
     * @param deployAppConfigVo
     */
    void deleteModifiedPartConfigDependency(DeployAppConfigVo deployAppConfigVo);
    /**
     * 找出修改部分配置信息
     * @param fullConfig 前端传过来的全量配置信息
     * @param parentConfig 如果当前层是环境层，parentConfig表示的是模块层修改部分配置信息；如果当前层是模块层，parentConfig应该为null。
     * @return
     */
    DeployPipelineConfigVo getModifiedPartConfig(DeployPipelineConfigVo fullConfig, DeployPipelineConfigVo parentConfig);
}
