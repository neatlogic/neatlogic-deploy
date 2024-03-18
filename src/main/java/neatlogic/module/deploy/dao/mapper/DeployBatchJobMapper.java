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
