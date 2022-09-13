/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.job.DeployJobVo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface DeployJobService {

    List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo);

    /**
     * 校验&&补充作业参数
     *
     * @param deployJobParam 入参
     * @param isBatch        是否批量
     */
    void initDeployParam(DeployJobVo deployJobParam, Boolean isBatch);

    /**
     * 转为自动化通用格式
     *
     * @param deployJobParam 入参
     */
    void convertModuleList(DeployJobVo deployJobParam);

    /**
     * 转为自动化通用格式
     *
     * @param deployJobParam 入参
     */
    void convertSingleModule(DeployJobVo deployJobParam);

    /**
     * 创建超级流水线发布作业
     *
     * @param autoexecJobParam 作业入参
     * @return result
     */
    JSONObject createBatchJob(DeployJobVo autoexecJobParam) throws Exception;


    /**
     * 创建发布作业
     *
     * @param autoexecJobParam 作业入参
     * @return result
     */
    JSONObject createJob(DeployJobVo autoexecJobParam) throws Exception;
    /**
     * 创建定时发布作业
     *
     * @param deployJobVo 入参
     * @return result
     */
    JSONObject createScheduleJob(DeployJobVo deployJobVo);


    /**
     * 获取来源id
     *
     * @param jsonObj 入参
     * @return 来源id
     */
    Long getOperationId(JSONObject jsonObj);


}
