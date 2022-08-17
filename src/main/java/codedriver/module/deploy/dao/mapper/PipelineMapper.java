/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.pipeline.*;

import java.util.List;

public interface PipelineMapper {
    List<PipelineVo> searchPipeline(PipelineVo pipelineVo);

    int searchPipelineCount(PipelineVo pipelineVo);

    PipelineJobTemplateVo getJobTemplateById(Long id);

    List<PipelineJobTemplateVo> searchJobTemplate(PipelineJobTemplateVo jobTemplateVo);

    int searchJobTemplateCount(PipelineJobTemplateVo jobTemplateVo);

    PipelineVo getPipelineById(Long id);

    void updatePipeline(PipelineVo pipelineVo);

    void insertPipelineAuth(PipelineAuthVo pipelineAuthVo);

    void insertPipeline(PipelineVo pipelineVo);

    void insertLane(PipelineLaneVo pipelineLaneVo);

    void insertJobTemplate(PipelineJobTemplateVo jobTemplateVo);

    void insertLaneGroup(PipelineGroupVo pipelineGroupVo);

    void insertPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo);

    void deletePipelineAuthByPipelineId(Long pipelineId);

    void deleteLaneGroupJobTemplateByPipelineId(Long pipelineId);
}
