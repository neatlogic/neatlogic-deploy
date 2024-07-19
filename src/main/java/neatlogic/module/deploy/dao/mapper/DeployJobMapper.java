/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

    void updateDeployJobBuildNoById(@Param("jobId") Long jobId,@Param("buildNo")String buildNo);

    void deleteDeployJobContentByHash(String contentHash);

    void deleteLaneGroupJobByJobId(Long jobId);

    void deleteJobAuthByJobId(Long jobId);

    void deleteJobInvokeByJobId(Long jobId);

    void deleteJobById(Long jobId);

}
