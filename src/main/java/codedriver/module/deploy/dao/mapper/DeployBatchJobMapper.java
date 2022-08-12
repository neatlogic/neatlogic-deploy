/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.framework.deploy.dto.job.LaneVo;
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
