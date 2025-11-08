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

import neatlogic.framework.deploy.dto.app.DeployBlueGreenVo;
import neatlogic.framework.deploy.dto.app.DeployInstanceBlueGreenVo;
import neatlogic.framework.deploy.dto.app.DeployJobResourceBlueGreenVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/12/8 15:07
 */

public interface DeployBlueGreenMapper {


    Integer getBlueGreenCount(DeployBlueGreenVo deployBlueGreenVo);

    List<DeployBlueGreenVo> searchBlueGreen(DeployBlueGreenVo deployBlueGreenVo);

    Integer updateBlueGreen(DeployBlueGreenVo deployBlueGreenVo);

    Integer insertBlueGreen(DeployBlueGreenVo deployBlueGreenVo);

    Integer insertInstanceBlueGreen(DeployInstanceBlueGreenVo instanceBlueGreenVo);

    List<DeployInstanceBlueGreenVo> listInstanceBlueGreen(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("resourceIdList") List<Long> instanceIdList);

    List<DeployJobResourceBlueGreenVo> listDeployJobResourceBlueGreen(Long jobId);

    int getDeployJobBlueGreenCountByJobId(Long jobId);

    void insertDeployJobResourceBlueGreen(@Param("list") List<DeployJobResourceBlueGreenVo> deployJobPhaseNodeBlueGreenVos);

    void deleteDeployJobResourceBlueGreen(@Param("list") List<DeployJobResourceBlueGreenVo> deployJobPhaseNodeBlueGreenVos);

    Integer deleteInstanceBlueGreen(DeployInstanceBlueGreenVo instanceBlueGreenVo);

    void deleteDeployJobResourceBlueGreenByJobId(Long jobId);

}
