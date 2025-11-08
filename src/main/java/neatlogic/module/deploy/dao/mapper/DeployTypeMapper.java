/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
