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

    void deleteDeployJobContentByHash(String contentHash);

    void deleteLaneGroupJobByJobId(Long jobId);

    void deleteJobAuthByJobId(Long jobId);

    void deleteJobInvokeByJobId(Long jobId);

    void deleteJobById(Long jobId);

}
