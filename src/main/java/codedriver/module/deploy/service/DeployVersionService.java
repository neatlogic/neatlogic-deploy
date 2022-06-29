/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.version.DeployVersionVo;
import com.alibaba.fastjson.JSONObject;

public interface DeployVersionService {

    String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version);
}
