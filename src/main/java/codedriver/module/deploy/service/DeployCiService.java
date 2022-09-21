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

    /**
     * 代码仓库hook回调创建发布作业
     *
     * @param paramObj      回调参数
     * @param ci            持续集成配置
     * @param versionName   版本号
     * @param deployVersion 版本
     * @param repoType      仓库类型
     * @throws Exception
     */
    void createJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

    /**
     * 代码仓库hook回调创建批量发布作业
     *
     * @param paramObj      回调参数
     * @param ci            持续集成配置
     * @param versionName   版本号
     * @param deployVersion 版本
     * @param repoType      仓库类型
     * @throws Exception
     */
    void createBatchJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

}
