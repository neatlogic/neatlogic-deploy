/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/13 11:05
 **/
public interface DeployPipelineTemplateMapper {

    Long checkPinelineTemplateNameIsRepeat(DeployPipelineTemplateVo autoexecCombopVo);

    Integer getPinelineTemplateIsActiveByIdForUpdate(Long id);

    DeployPipelineTemplateVo getPinelineTemplateById(Long id);

    int getPinelineTemplateCount(DeployPipelineTemplateVo searchVo);

    List<DeployPipelineTemplateVo> getPinelineTemplateList(DeployPipelineTemplateVo searchVo);

    List<Long> checkPinelineTemplateIdListIsExists(List<Long> idList);

    int insertPinelineTemplate(DeployPipelineTemplateVo autoexecCombopVo);

    int updatePinelineTemplateById(DeployPipelineTemplateVo autoexecCombopVo);

    int updatePinelineTemplateIsActiveById(DeployPipelineTemplateVo autoexecCombopVo);

    int deletePinelineTemplateById(Long id);

}
