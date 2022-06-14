/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppPipelineGetApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID")
    })
    @Output({
            @Param(name = "Return", explode = DeployAppConfigVo.class, desc = "应用流水线配置信息")
    })
    @Description(desc = "获取应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        String targetLevel = null;
        DeployPipelineConfigVo appConfig = null;
        DeployPipelineConfigVo moduleOverrideConfig = null;
        DeployPipelineConfigVo envOverrideConfig = null;
        Long appSystemId = searchVo.getAppSystemId();
        Long moduleId = searchVo.getModuleId();
        Long envId = searchVo.getEnvId();
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            deployAppConfigVo = searchVo;
        }
        String overrideConfigStr = deployAppConfigVo.getConfigStr();
        if (moduleId == 0L && envId == 0L) {
            targetLevel = "应用";
            //查询应用层流水线配置信息
            if (StringUtils.isBlank(overrideConfigStr)) {
                throw new DeployAppConfigNotFoundException(appSystemId);
            }
            appConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
        } else if (moduleId == 0L && envId != 0L) {
            // 如果是访问环境层配置信息，moduleId不能为空
            throw new ParamNotExistsException("moduleId");
        } else if (moduleId != 0L && envId == 0L) {
            targetLevel = "模块";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            if (StringUtils.isNotBlank(overrideConfigStr)) {
                moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
            }
        } else {
            targetLevel = "环境";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId, moduleId));
            if (StringUtils.isNotBlank(moduleOverrideConfigStr)) {
                moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
            }
            if (StringUtils.isNotBlank(overrideConfigStr)) {
                envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
            }
        }
        DeployPipelineConfigVo deployPipelineConfigVo = deployAppPipelineService.mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel);
        deployAppConfigVo.setConfig(deployPipelineConfigVo);
        return deployAppConfigVo;
    }
}
