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
     * @param jsonObj 入参
     */
    void initDeployParam(JSONObject jsonObj);

    /**
     * 转为自动化通用格式
     *
     * @param jsonObj    入参
     * @param moduleJson 模块入参
     */
    void convertModule(JSONObject jsonObj, JSONObject moduleJson);

    /**
     * 创建发布作业
     *
     * @param jsonObj 作业入参
     * @return result
     */
    JSONObject createJob(JSONObject jsonObj) throws Exception;


    /**
     * 创建定时发布作业
     * @param jsonObj 入参
     * @return result
     */
    JSONObject createScheduleJob(JSONObject jsonObj);


    /**
     * 获取来源id
     *
     * @param jsonObj 入参
     * @return 来源id
     */
    Long getOperationId(JSONObject jsonObj);


}
