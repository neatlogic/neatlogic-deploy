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
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DeployJobService {

    List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo);

    /**
     * 转为自动化通用格式
     *
     * @param deployJobParam 入参
     */
    void convertModule(DeployJobVo deployJobParam);

    /**
     * 转为自动化通用格式
     *
     * @param deployJobParam 入参
     * @param moduleVo       模块
     */
    void convertModule(DeployJobVo deployJobParam, DeployJobModuleVo moduleVo);

    /**
     * 创建超级流水线发布作业
     *
     * @param autoexecJobParam 作业入参
     * @return result
     */
    JSONObject createJob(DeployJobVo autoexecJobParam) throws Exception;

    /**
     * 创建超级流水线发布作业
     *
     * @param autoexecJobParam 作业入参
     * @param module           模块
     * @return result
     */
    JSONObject createJob(DeployJobVo autoexecJobParam, DeployJobModuleVo module) throws Exception;

    /**
     * 创建发布作业,用于moduleList 格式
     *
     * @param autoexecJobParam 作业入参
     * @param module           模块
     * @return result
     */
    @Transactional
    JSONObject createJobAndFire(DeployJobVo autoexecJobParam, DeployJobModuleVo module) throws Exception;

    /**
     * 创建发布作业
     *
     * @param autoexecJobParam 作业入参
     * @return result
     */
    JSONObject createJobAndFire(DeployJobVo autoexecJobParam) throws Exception;

    /**
     * 查询发布作业状态
     *
     * @param jobId 作业id
     * @return 状态信息
     */
    JSONObject getJobStatus(Long jobId);

    /**
     * 创建定时发布作业
     *
     * @param deployJobVo 入参
     * @param module      模块
     * @return result
     */
    JSONObject createJobAndSchedule(DeployJobVo deployJobVo, DeployJobModuleVo module);


    /**
     * 获取来源id
     *
     * @param jsonObj 入参
     * @return 来源id
     */
    Long getOperationId(JSONObject jsonObj);

    /**
     * 校验发布定时作业权限
     * @param scheduleVo 发布定时作业
     */
    void scheduleAuthCheck(DeployScheduleVo scheduleVo);
}
