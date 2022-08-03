package codedriver.module.deploy.service;

import com.alibaba.fastjson.JSONObject;

public interface DeployAppConfigAuthorityService {

    /**
     * 根据系统id获取权限列表
     * @param appSystemId 系统id
     * @return 权限列表
     */
    JSONObject getAuthorityListBySystemId(Long appSystemId);
}
