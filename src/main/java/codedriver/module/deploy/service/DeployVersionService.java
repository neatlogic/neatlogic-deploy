/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
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
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     */
    String getWorkspaceRunnerUrl(Long appSystemId, Long appModuleId);

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

}
