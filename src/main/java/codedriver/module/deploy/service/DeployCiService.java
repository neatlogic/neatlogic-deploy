/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.deploy.constvalue.DeployCiRepoType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.dto.runner.RunnerVo;
import com.alibaba.fastjson.JSONObject;

public interface DeployCiService {

    /**
     * 随机挑选模块下的runner
     *
     * @param system 系统
     * @param module 模块
     * @return
     */
    RunnerVo getRandomRunnerBySystemIdAndModuleId(CiEntityVo system, CiEntityVo module);

    void createJobForCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

    void createBatchJobForCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

}
