/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.crossover.IDeployTypeCrossoverMapper;
import neatlogic.framework.deploy.dto.type.DeployTypeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/12/8 15:07
 */

public interface DeployTypeMapper extends IDeployTypeCrossoverMapper {

    List<DeployTypeVo> searchType(DeployTypeVo deployTypeSearchVo);

    int searchTypeCount(DeployTypeVo deployTypeSearchVo);

    void insertTypeActive(@Param("isActive") Integer isActive, @Param("id") Long id);

    void deleteTypeActiveByTypeId(Long typeId);
}
