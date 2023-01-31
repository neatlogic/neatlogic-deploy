/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.integration.handler;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.integration.core.IntegrationHandlerBase;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.integration.dto.PatternVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeployTriggerIntegrationHandler extends IntegrationHandlerBase {
    @Override
    public String getName() {
        return "发布触发器数据规范";
    }

    @Override
    public Integer hasPattern() {
        return 1;
    }

    @Override
    public List<PatternVo> getInputPattern() {
        List<PatternVo> jsonList = new ArrayList<>();
        jsonList.add(new PatternVo("appSystemId", "input", ApiParamType.LONG, 0, "应用系统id",0));
        jsonList.add(new PatternVo("appSystemName", "input", ApiParamType.STRING, 0, "应用系统名",0));
        jsonList.add(new PatternVo("appSystemAbbrName", "input", ApiParamType.STRING, 0, "应用系统简称",0));
        jsonList.add(new PatternVo("appModuleId", "input", ApiParamType.LONG, 0, "应用模块id",0));
        jsonList.add(new PatternVo("appModuleName", "input", ApiParamType.STRING, 0, "应用模块名",0));
        jsonList.add(new PatternVo("appModuleAbbrName", "input", ApiParamType.STRING, 0, "应用模块简称",0));
        jsonList.add(new PatternVo("envName", "input", ApiParamType.STRING, 0, "目标环境名",0));
        jsonList.add(new PatternVo("buildNo", "input", ApiParamType.INTEGER, 0, "编译号",0));
        jsonList.add(new PatternVo("scenarioName", "input", ApiParamType.STRING, 0, "场景"));
        jsonList.add(new PatternVo("targetEnvName", "input", ApiParamType.STRING, 0, "目标环境"));
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
