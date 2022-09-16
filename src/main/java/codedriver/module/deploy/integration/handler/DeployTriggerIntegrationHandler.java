/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.integration.handler;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.integration.core.IntegrationHandlerBase;
import codedriver.framework.integration.dto.IntegrationResultVo;
import codedriver.framework.integration.dto.IntegrationVo;
import codedriver.framework.integration.dto.PatternVo;

import java.util.ArrayList;
import java.util.List;

public class DeployTriggerIntegrationHandler extends IntegrationHandlerBase {
    @Override
    public String getName() {
        return "发布触发器组件";
    }

    @Override
    public Integer hasPattern() {
        return 1;
    }

    @Override
    public List<PatternVo> getInputPattern() {
        List<PatternVo> jsonList = new ArrayList<>();
        jsonList.add(new PatternVo("appSystemId", "input", ApiParamType.LONG, 0, "应用系统id"));
        jsonList.add(new PatternVo("appSystemName", "input", ApiParamType.STRING, 0, "应用系统名"));
        jsonList.add(new PatternVo("appSystemAbbrName", "input", ApiParamType.STRING, 0, "应用系统简称"));
        jsonList.add(new PatternVo("appModuleId", "input", ApiParamType.LONG, 0, "应用模块id"));
        jsonList.add(new PatternVo("appModuleName", "input", ApiParamType.STRING, 0, "应用模块名"));
        jsonList.add(new PatternVo("appModuleAbbrName", "input", ApiParamType.STRING, 0, "应用模块简称"));
        jsonList.add(new PatternVo("envName", "input", ApiParamType.STRING, 0, "目标环境名"));
        jsonList.add(new PatternVo("buildNo", "input", ApiParamType.INTEGER, 0, "编译号"));
        jsonList.add(new PatternVo("scenarioName", "input", ApiParamType.INTEGER, 0, "场景名"));
        return jsonList;
    }

    @Override
    public List<PatternVo> getOutputPattern() {
        return null;
    }

    @Override
    public void validate(IntegrationResultVo resultVo) throws ApiRuntimeException {

    }

    @Override
    protected void beforeSend(IntegrationVo integrationVo) {

    }

    @Override
    protected void afterReturn(IntegrationVo integrationVo) {

    }
}
