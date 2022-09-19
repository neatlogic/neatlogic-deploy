/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.pipeline.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployPipelineMapper {
    List<PipelineVo> searchPipeline(PipelineVo pipelineVo);

    int searchPipelineCount(PipelineVo pipelineVo);

    PipelineJobTemplateVo getJobTemplateById(Long id);

    List<PipelineJobTemplateVo> searchJobTemplate(PipelineJobTemplateVo jobTemplateVo);

    int searchJobTemplateCount(PipelineJobTemplateVo jobTemplateVo);

    PipelineVo getPipelineById(Long id);

    PipelineVo getPipelineSimpleInfoById(Long id);

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
