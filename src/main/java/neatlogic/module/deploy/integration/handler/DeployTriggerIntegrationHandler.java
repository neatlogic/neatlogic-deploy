/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
