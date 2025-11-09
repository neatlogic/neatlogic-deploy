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
