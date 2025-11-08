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

import neatlogic.framework.deploy.dto.app.DeployAppSystemVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployAppSystemMapper {

    List<Long> getAppSystemIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> getAppSystemListIncludeModuleByIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> getAppSystemListByIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    Integer getAppSystemIdListCount(DeployResourceSearchVo searchVo);
}
