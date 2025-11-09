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

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;

public interface DeployBatchJobService {

    void creatBatchJob(DeployJobVo deployJobVo, PipelineVo pipelineVo) throws Exception;

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
     * @param groupId 组id
     * @param isGoon  执行完当前组是否停止不继续执行后续组，但仍受needWait约束
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

    /**
     * 是否存在超级流水线权限
     *
     */
    void isHasPipelineAuth(Long appSystemId, Long pipelineId);

    /**
     * 是否存在超级流水线权限
     *
     * @param jobId 批量作业的父id
     */
    void isJobHasPipelineAuth(Long jobId);
}
