/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.service;

import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.deploy.constvalue.DeployCiRepoType;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.dto.runner.RunnerVo;
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
     * @return 作业id
     * @throws Exception
     */
    Long createJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

    /**
     * 代码仓库hook回调创建批量发布作业
     *
     * @param paramObj      回调参数
     * @param ci            持续集成配置
     * @param versionName   版本号
     * @param deployVersion 版本
     * @param repoType      仓库类型
     * @return 作业id
     * @throws Exception
     */
    Long createBatchJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

    /**
     * 删除gitlab webhook
     *
     * @param ci        持续集成配置
     * @param runnerUrl runner url
     */
    void deleteGitlabWebHook(DeployCiVo ci, String runnerUrl);

}
