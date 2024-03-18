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
