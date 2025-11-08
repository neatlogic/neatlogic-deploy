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

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployResourceMapper {

    List<ResourceVo> getAppInstanceResourceListByIdList(@Param("idList") List<Long> idList);

    List<ResourceVo> getAppInstanceResourceListByIdListAndKeyword(@Param("idList") List<Long> idList, @Param("keyword") String keyword);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdListAndEnvId(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<Long> getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

    Integer getAppInstanceResourceIdCountByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);
}
