/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.deploy.constvalue.DeployCiRepoType;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.dto.runner.RunnerVo;

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
    String createJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

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
    String createBatchJobForVCSCallback(JSONObject paramObj, DeployCiVo ci, String versionName, DeployVersionVo deployVersion, DeployCiRepoType repoType) throws Exception;

    /**
     * 删除gitlab webhook
     *
     * @param ci        持续集成配置
     * @param runnerUrl runner url
     */
    void deleteGitlabWebHook(DeployCiVo ci, String runnerUrl);

}
