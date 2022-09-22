/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployModuleScenarioAndEnvListApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "/deploy/module/scenarioandenvlist/get";
    }

    @Override
    public String getName() {
        return "获取应用模块的场景与环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id")
    })
    @Output({
    })
    @Description(desc = "获取应用模块的场景与环境列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId");
        //判断是否有配流水线
        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isEmpty(appConfigVoList)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        //场景
        DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId)
                .withAppModuleId(appModuleId)
                .getConfig();
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        result.put("scenarioList", pipelineConfigVo.getScenarioList());
        result.put("defaultScenarioId", pipelineConfigVo.getDefaultScenarioId());
        result.put("envList", deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndModuleId(appSystemId, appModuleId));
        return result;
    }
}
