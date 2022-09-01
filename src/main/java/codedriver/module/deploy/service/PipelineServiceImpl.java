/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public void setDeployPipelineJobTemplateAppSystemNameAndAppModuleName(List<PipelineJobTemplateVo> pipelineJobTemplateVoList) {
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        TenantContext.get().switchDataDatabase();
        List<ResourceVo> appSystemResourceList = iResourceCrossoverMapper.searchAppSystemListByIdList(new ArrayList<>(pipelineJobTemplateVoList.stream().map(PipelineJobTemplateVo::getAppSystemId).collect(Collectors.toSet())));
        List<ResourceVo> appModuleResourceList = iResourceCrossoverMapper.getAppModuleListByIdListSimple(new ArrayList<>(pipelineJobTemplateVoList.stream().map(PipelineJobTemplateVo::getAppModuleId).collect(Collectors.toSet())));
        TenantContext.get().switchDefaultDatabase();
        Map<Long, ResourceVo> appSystemResourceMap = appSystemResourceList.stream().collect(Collectors.toMap(ResourceVo::getId, e -> e));
        Map<Long, ResourceVo> appModuleResourceMap = appModuleResourceList.stream().collect(Collectors.toMap(ResourceVo::getId, e -> e));
        for (PipelineJobTemplateVo jobVo : pipelineJobTemplateVoList) {
            ResourceVo appSystemResourceVo = appSystemResourceMap.get(jobVo.getAppSystemId());
            if (appSystemResourceVo != null) {
                jobVo.setAppSystemName(appSystemResourceVo.getName());
                jobVo.setAppSystemAbbrName(appSystemResourceVo.getAbbrName());
            }
            ResourceVo appModuleResourceVo = appModuleResourceMap.get(jobVo.getAppModuleId());
            if (appModuleResourceVo != null) {
                jobVo.setAppModuleName(appModuleResourceVo.getName());
                jobVo.setAppModuleAbbrName(appModuleResourceVo.getAbbrName());
            }
        }
    }
}
