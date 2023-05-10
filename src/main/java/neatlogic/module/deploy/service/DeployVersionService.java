/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.service;

import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import com.alibaba.fastjson.JSONObject;

public interface DeployVersionService {

    /**
     * 根据版本ID、buildNo或envId查询runner地址
     *
     * @param paramObj
     * @param version
     * @return
     */
    String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version, String envName);

    /**
     * 根据版本、资源类型、builNo或envName决定文件完整路径
     *
     * @param version      版本
     * @param resourceType 资源类型
     * @param buildNo
     * @param envName
     * @param customPath
     * @return
     */
    String getVersionResourceFullPath(DeployVersionVo version, DeployResourceType resourceType, Integer buildNo, String envName, String customPath);

    String getVersionResourceHomePath(DeployVersionVo version, DeployResourceType resourceType, Integer buildNo, String envName);

    /**
     * 获取工程目录runner地址
     *
     * @param versionVo 版本
     */
    String getWorkspaceRunnerUrl(DeployVersionVo versionVo);

    /**
     * 获取工程目录根路径
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    String getWorkspaceResourceHomePath(Long appSystemId, Long appModuleId);

    /**
     * 获取工程目录下文件完整路径
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @param customPath
     * @return
     */
    String getWorkspaceResourceFullPath(Long appSystemId, Long appModuleId, String customPath);

    /**
     * 对HOME目录进行打包下载时，HOME目录将被锁定，此时除查看文件外，不允许对HOME目录下的文件做任何操作
     *
     * @param runnerUrl runner地址
     * @param path      HOME目录路径
     */
    void checkHomeHasBeenLocked(String runnerUrl, String path);

    /**
     * 获取环境名称
     *
     * @param version
     * @param envId
     * @return
     */
    String getEnvName(String version, Long envId);

}
