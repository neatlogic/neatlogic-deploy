/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppPipelineApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;
    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID")
    })
    @Output({
            @Param(name = "Return", explode = DeployAppConfigVo.class, desc = "应用流水线配置信息")
    })
    @Description(desc = "获取应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        String schemaName = TenantContext.get().getDataDbName();
        ResourceVo appSystem = resourceCenterMapper.getAppSystemById(searchVo.getAppSystemId(), schemaName);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(searchVo.getAppSystemId());
        }
        searchVo.setAppSystemName(appSystem.getName());
        Long appModuleId = searchVo.getAppModuleId();
        if (appModuleId != null && appModuleId != 0) {
            ResourceVo appModule = resourceCenterMapper.getAppModuleById(appModuleId, schemaName);
            if (appModule == null) {
                throw new AppModuleNotFoundException(appModuleId);
            }
            searchVo.setAppModuleName(appModule.getName());
        }
        Long envId = searchVo.getEnvId();
        if (envId != null && envId != 0) {
            ResourceVo env = resourceCenterMapper.getAppEnvById(envId, schemaName);
            if (env == null) {
                throw new AppEnvNotFoundException(envId);
            }
            searchVo.setEnvName(env.getName());
        }
        DeployPipelineConfigVo deployPipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(searchVo);
        searchVo.setConfig(deployPipelineConfigVo);
        return searchVo;
    }
}
