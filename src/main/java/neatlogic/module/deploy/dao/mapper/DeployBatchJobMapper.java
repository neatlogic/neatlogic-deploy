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

import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.dto.job.LaneVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lvzk
 * @date 2022/8/3 16:19 下午
 */
public interface DeployBatchJobMapper {
    DeployJobVo getBatchDeployJobLockById(Long batchJobId);

    List<AutoexecJobVo> getBatchDeployJobListByIdAndNotInStatus(@Param("id") Long batchJobId, @Param("statusList") List<String> statusList);

    List<AutoexecJobVo> getJobsByGroupIdAndWithoutStatus(@Param("groupId") Long groupId, @Param("statusList") List<String> statusList);

    DeployJobVo getBatchJobByGroupId(@Param("groupId") Long groupId);

    List<LaneVo> getLaneListByBatchJobId(Long batchJobId);

    LaneGroupVo getLaneGroupByGroupId(Long groupId);

    LaneGroupVo getLaneGroupByJobId(Long jobId);

    LaneVo getLaneById(Long laneId);

    Long getLaneFireGroupId(@Param("batchJobId") Long batchJobId, @Param("laneId") Long laneId);

    Long getNextGroupId(@Param("laneId")Long laneId,@Param("currentSort") Integer sort);

    void updateLaneStatus(@Param("laneId") Long id, @Param("laneStatus") String value);

    void updateGroupStatusByLaneId(@Param("laneId") Long id, @Param("groupStatus") String value);

    void updateGroupStatus(@Param("group") LaneGroupVo groupVo);

    void updateBatchJobStatusByGroupId(@Param("groupId") Long groupId, @Param("status") String status);
}
