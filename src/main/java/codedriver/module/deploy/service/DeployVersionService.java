/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

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
    String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version);

    /**
     * 根据版本、资源类型、builNo或envId决定文件完整路径
     *
     * @param version      版本
     * @param resourceType 资源类型
     * @param buildNo
     * @param envId
     * @param customPath
     * @return
     */
    String getVersionResourceFullPath(DeployVersionVo version, String resourceType, Integer buildNo, Long envId, String customPath);
}
