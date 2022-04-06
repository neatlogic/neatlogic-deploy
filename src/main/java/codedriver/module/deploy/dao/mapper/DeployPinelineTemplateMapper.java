/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.pinelinetemplate.DeployPinelineTemplateVo;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/13 11:05
 **/
public interface DeployPinelineTemplateMapper {

    Long checkPinelineTemplateNameIsRepeat(DeployPinelineTemplateVo autoexecCombopVo);

    Integer getPinelineTemplateIsActiveByIdForUpdate(Long id);

    DeployPinelineTemplateVo getPinelineTemplateById(Long id);

    int getPinelineTemplateCount(DeployPinelineTemplateVo searchVo);

    List<DeployPinelineTemplateVo> getPinelineTemplateList(DeployPinelineTemplateVo searchVo);

    List<Long> checkPinelineTemplateIdListIsExists(List<Long> idList);

    int insertPinelineTemplate(DeployPinelineTemplateVo autoexecCombopVo);

    int updatePinelineTemplateById(DeployPinelineTemplateVo autoexecCombopVo);

    int updatePinelineTemplateIsActiveById(DeployPinelineTemplateVo autoexecCombopVo);

    int deletePinelineTemplateById(Long id);

}
