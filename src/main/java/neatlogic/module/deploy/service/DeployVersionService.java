/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
