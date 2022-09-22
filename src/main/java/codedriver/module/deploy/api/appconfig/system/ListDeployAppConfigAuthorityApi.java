/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/6/15 11:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAuthorityApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "/deploy/app/config/authority/action/list";
    }

    @Override
    public String getName() {
        return "查询应用系统权限列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id")
    })
    @Output({
    })
    @Description(desc = "查询应用系统权限列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        JSONObject returnObj = new JSONObject();
        //操作权限
        returnObj.put("operationAuthList", DeployAppConfigAction.getValueTextList());

        //场景权限
        DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId).getConfig();
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        List<JSONObject> scenarioAuthList = new ArrayList<>();
        for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
            JSONObject scenarioValueText = new JSONObject();
            scenarioValueText.put("text", scenarioVo.getScenarioName());
            scenarioValueText.put("value", scenarioVo.getScenarioId());
            scenarioAuthList.add(scenarioValueText);
        }
        returnObj.put("scenarioAuthList", scenarioAuthList);

        //环境权限
        List<JSONObject> envAuthList = new ArrayList<>();
        List<DeployAppEnvironmentVo> envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>());
        for (DeployAppEnvironmentVo env : envList) {
            JSONObject envValueText = new JSONObject();
            envValueText.put("text", env.getName());
            envValueText.put("value", env.getId());
            envAuthList.add(envValueText);
        }
        returnObj.put("envAuthList", envAuthList);
        return returnObj;
    }
}
