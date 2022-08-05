/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.job.LaneGroupVo;

public interface DeployBatchJobService {
    /**
     * 执行批量作业
     *
     * @param batchJobId 批量作业id
     */
    void fireBatch(Long batchJobId);


    /**
     * 激活泳道组
     *
     * @param groupId 组id
     */
    void fireLaneGroup(Long groupId) throws Exception;

    /**
     *
     * @param groupId 组id
     * @param needWait 执行完该组后是否需要等待，即不继续激活下一组，1：是，0：否
     */
    public void fireLaneGroup(Long groupId, int needWait);

    /**
     * 激活泳道
     *
     * @param groupVo  组
     * @param isRefire 是否重跑
     */
    void fireLaneGroup(LaneGroupVo groupVo, boolean isRefire) throws Exception;

    /**
     * 检查并激活下一个组
     *
     * @param groupVo 组
     */
    void checkAndFireLaneNextGroup(LaneGroupVo groupVo);

    /**
     * 检查并激活下一个组
     *
     * @param groupId 组id
     */
    void checkAndFireLaneNextGroup(Long groupId);
}
