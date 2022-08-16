/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
}
