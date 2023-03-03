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
