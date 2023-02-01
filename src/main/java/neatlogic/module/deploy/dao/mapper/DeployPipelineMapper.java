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
