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

import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.deploy.dto.job.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lvzk
 * @date 2022/6/27 16:19 下午
 */
public interface DeployJobMapper {

    DeployJobVo getDeployJobByJobId(Long id);

    DeployJobVo getDeployJobInfoByJobId(Long id);

    List<DeployJobVo> getDeployJobByJobIdList(List<Long> idList);

    List<AutoexecJobVo> getDeploySubJobListByFilter(DeployJobVo deployJobVo);

    int searchDeployJobCount(DeployJobVo deployJobVo);

    List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo);

    List<Long> getJobIdListByParentId(Long parentId);

    List<Long> searchDeployJobId(DeployJobVo deployJobVo);

    /**
     * 根据应用ID和模块ID查询最近一次作业的runner_map_id
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    Long getRecentlyJobRunnerMapIdByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployJobVo> getDeployJobListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployJobVo> getDeployJobListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    DeployJobContentVo getDeployJobContentLock(String contentHash);

    DeployJobVo getBatchDeployJobById(Long id);

    DeployJobVo getJobBaseInfoById(Long id);

    List<LaneGroupVo> getDeployJobGroupByJobIdList(@Param("idList") List<Long> idList);

    DeployJobContentVo getDeployJobContent(String contentHash);

    int getDeployJobAuthCountByJobIdAndUuid(@Param("jobId") Long id, @Param("userUuid") String userUuid);

    void insertDeployJob(DeployJobVo deployJobVo);

    void insertAutoExecJob(DeployJobVo deployJobVo);

    void insertIgnoreDeployJobContent(DeployJobContentVo deployJobContentVo);

    void insertLane(LaneVo laneVo);

    void insertLaneGroup(LaneGroupVo laneGroupVo);

    void insertDeployJobAuth(DeployJobAuthVo deployJobAuthVo);

    void insertGroupJob(@Param("groupId") Long groupId, @Param("jobId") Long jobId, @Param("sort") Integer sort);

    void insertJobInvoke(@Param("jobId") Long jobId, @Param("invokeId") Long invokeId, @Param("source") String source, @Param("routeId") String routeId);

    void updateAutoExecJob(DeployJobVo deployJobVo);

    void resetAutoexecJobParentId(Long jobId);

    void updateDeployJobReviewStatusById(DeployJobVo deployJobVo);

    void updateDeployJobRunnerMapId(@Param("jobId") Long jobId, @Param("runnerMapId") Long runnerMapId);

    void updateAutoExecJobParentIdById(DeployJobVo deployJobVo);

    void updateDeployJobBuildNoById(@Param("jobId") Long jobId,@Param("buildNo")String buildNo);

    void deleteDeployJobContentByHash(String contentHash);

    void deleteLaneGroupJobByJobId(Long jobId);

    void deleteJobAuthByJobId(Long jobId);

    void deleteJobInvokeByJobId(Long jobId);

    void deleteJobById(Long jobId);

}
