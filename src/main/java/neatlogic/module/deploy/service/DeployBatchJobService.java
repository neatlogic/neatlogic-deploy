/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.service;

import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import com.alibaba.fastjson.JSONObject;

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
