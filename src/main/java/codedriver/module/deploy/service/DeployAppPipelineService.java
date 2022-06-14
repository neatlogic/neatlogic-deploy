/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;

import java.util.List;

public interface DeployAppPipelineService {

//    /**
//     * 查询应用、模块、环境的流水线配置信息
//     * @param deployAppConfigVo
//     * @return
//     */
//    DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo deployAppConfigVo);
//
//    /**
//     * 查询应用、模块、环境的流水线配置信息
//     * @param deployAppConfigVo
//     * @param profileIdList
//     * @return
//     */
//    DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo deployAppConfigVo, List<Long> profileIdList);
    /**
     * 组装应用、模块、环境的流水线配置信息
     * @param appConfig
     * @param moduleOverrideConfig
     * @param envOverrideConfig
     * @param targetLevel
     * @return
     */
    DeployPipelineConfigVo mergeDeployPipelineConfigVo(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel);
    /**
     * 组装应用、模块、环境的流水线配置信息
     * @param appConfig
     * @param moduleOverrideConfig
     * @param envOverrideConfig
     * @param targetLevel
     * @param profileIdList
     * @return
     */
    DeployPipelineConfigVo mergeDeployPipelineConfigVo(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel, List<Long> profileIdList);
}
