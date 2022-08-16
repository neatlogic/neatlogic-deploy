/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.apppipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineProfileApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取应用流水线预置参数集列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/List";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值")
    })
    @Output({
            @Param(name = "Return", explode = DeployProfileVo[].class, desc = "应用流水线预置参数集列表")
    })
    @Description(desc = "获取应用流水线预置参数集列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        String targetLevel = null;
        DeployPipelineConfigVo appConfig = null;
        DeployPipelineConfigVo moduleOverrideConfig = null;
        DeployPipelineConfigVo envOverrideConfig = null;
        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();
        String overrideConfigStr = deployAppConfigMapper.getAppConfig(deployAppConfigVo);
        if (moduleId == 0L && envId == 0L) {
            targetLevel = "应用";
            //查询应用层流水线配置信息
            if (StringUtils.isBlank(overrideConfigStr)) {
                overrideConfigStr = "{}";
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
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> profileIdList = defaultValue.toJavaList(Long.class);
            DeployPipelineConfigVo config = deployAppPipelineService.mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel, profileIdList);
            return config.getOverrideProfileList();
        } else {
            DeployPipelineConfigVo config = deployAppPipelineService.mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel);
            return config.getOverrideProfileList();
        }
    }
}
