/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/15 11:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAuthorityApi extends PrivateApiComponentBase {
    @Resource
    DeployAppPipelineService deployAppPipelineService;

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
        JSONArray resultArray = DeployAppConfigAction.getValueTextList();
        Long appSystemId = paramObj.getLong("appSystemId");
        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));

        if(CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())){
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                JSONObject scenarioKeyValue = new JSONObject();
                scenarioKeyValue.put("value", scenarioVo.getScenarioName());
                scenarioKeyValue.put("text", scenarioVo.getScenarioName());
                resultArray.add(scenarioKeyValue);
            }
        }
        return resultArray;
    }
}
