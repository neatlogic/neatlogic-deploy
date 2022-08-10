/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.job.LaneGroupVo;
import com.alibaba.fastjson.JSONObject;

public interface DeployBatchJobService {
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
     * 激活该泳道下一组
     *
     * @param currentGroupVo 当前组
     * @param nextGroupId    下一组id
     */
    void fireLaneNextGroup(LaneGroupVo currentGroupVo, Long nextGroupId, JSONObject passThroughEnv);
}
