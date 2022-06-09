/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.app.DeployAppConfigOverrideVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;

public interface DeployAppPipelineService {

    DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigOverrideVo deployAppConfigOverrideVo);
}
