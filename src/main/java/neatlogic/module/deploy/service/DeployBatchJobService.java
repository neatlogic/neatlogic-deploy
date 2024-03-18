/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;

public interface DeployBatchJobService {

    void creatBatchJob(DeployJobVo deployJobVo, PipelineVo pipelineVo, boolean isFire) throws Exception;
    /**
     * 执行批量作业
     *
     * @param batchJobId 批量作业id
     */
    void fireBatch(Long batchJobId, String batchJobAction, String jobAction);


    /**
     * 激活泳道组
     *
     * @param groupId 组id
     */
    void fireLaneGroup(Long groupId, String batchJobAction, String jobAction, JSONObject passThroughEnv) throws Exception;

    /**
     * @param groupId  组id
     * @param isGoon 执行完当前组是否停止不继续执行后续组，但仍受needWait约束
     */
    void refireLaneGroup(Long groupId, int isGoon, String batchJobAction, String jobAction);

    /**
     * 激活泳道
     *
     * @param groupVo  组
     * @param isRefire 是否重跑
     */
    void fireLaneGroup(LaneGroupVo groupVo, int isRefire, JSONObject passThroughEnv) throws Exception;

    /**
     * 检查并激活下一个组
     *
     * @param groupVo 组
     */
    void checkAndFireLaneNextGroup(LaneGroupVo groupVo, JSONObject passThroughEnv);

    /**
     * 检查并激活下一个组
     *
     * @param groupId 组id
     */
    void checkAndFireLaneNextGroup(Long groupId, JSONObject passThroughEnv);


    /**
     * 检查并激活下一个组
     *
     * @param jobId 作业id
     */
    void checkAndFireLaneNextGroupByJobId(Long jobId, JSONObject passThroughEnv);

    /**
     * 激活该泳道下一组
     *
     * @param currentGroupVo 当前组
     * @param nextGroupId    下一组id
     */
    void fireLaneNextGroup(LaneGroupVo currentGroupVo, Long nextGroupId, JSONObject passThroughEnv);
}
