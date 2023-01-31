/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.service;

import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import com.alibaba.fastjson.JSONObject;

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
    JSONObject createJobAndFire(DeployJobVo autoexecJobParam, DeployJobModuleVo module) throws Exception;

    /**
     * 创建发布作业
     *
     * @param autoexecJobParam 作业入参
     * @return result
     */
    JSONObject createJobAndFire(DeployJobVo autoexecJobParam) throws Exception;

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


}
