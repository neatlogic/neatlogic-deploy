/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
