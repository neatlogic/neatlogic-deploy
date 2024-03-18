/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.deploy.dto.job.DeployJobModuleVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;

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
