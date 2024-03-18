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

package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.pipeline.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployPipelineMapper {
    List<PipelineVo> searchPipeline(PipelineSearchVo searchVo);

    int searchPipelineCount(PipelineSearchVo searchVo);

    PipelineJobTemplateVo getJobTemplateById(Long id);

    List<PipelineJobTemplateVo> searchJobTemplate(PipelineJobTemplateVo jobTemplateVo);

    int searchJobTemplateCount(PipelineJobTemplateVo jobTemplateVo);

    PipelineVo getPipelineById(Long id);

    PipelineVo getPipelineSimpleInfoById(Long id);

    List<PipelineVo> getPipelineListByIdList(List<Long> idList);

    /**
     * 根据流水线id和模块id获取流水线与其中属于{moduleId}的作业模版
     *
     * @param id       流水线id
     * @param moduleId 模块id
     * @return
     */
    PipelineVo getPipelineBaseInfoByIdAndModuleId(@Param("id") Long id, @Param("moduleId") Long moduleId);

    String getPipelineNameById(Long id);

    int checkPipelineNameIsExists(PipelineVo pipelineVo);

    List<Long> checkHasAuthPipelineIdList(@Param("pipelineIdList") List<Long> pipelineIdList, @Param("authUuid") String authUuid);

    void updatePipeline(PipelineVo pipelineVo);

    void insertPipelineAuth(PipelineAuthVo pipelineAuthVo);

    void insertPipeline(PipelineVo pipelineVo);

    void insertLane(PipelineLaneVo pipelineLaneVo);

    void insertJobTemplate(PipelineJobTemplateVo jobTemplateVo);

    void insertLaneGroup(PipelineGroupVo pipelineGroupVo);

    void insertPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo);

    void deletePipelineById(Long pipelineId);

    void deletePipelineAuthByPipelineId(Long pipelineId);

    void deleteLaneGroupJobTemplateByPipelineId(Long pipelineId);
}
