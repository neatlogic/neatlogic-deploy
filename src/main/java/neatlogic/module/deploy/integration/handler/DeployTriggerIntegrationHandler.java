/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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

import neatlogic.framework.util.$;
@Component
public class DeployTriggerIntegrationHandler extends IntegrationHandlerBase {
    @Override
    public String getName() {
        return "nmdih.deploytriggerintegrationhandler.getname";
    }

    @Override
    public Integer hasPattern() {
        return 1;
    }

    @Override
    public List<PatternVo> getInputPattern() {
        List<PatternVo> jsonList = new ArrayList<>();
        jsonList.add(new PatternVo("appSystemId", "input", ApiParamType.LONG, 0, $.t("term.cmdb.appsystemid"),0));
        jsonList.add(new PatternVo("appSystemName", "input", ApiParamType.STRING, 0, $.t("term.cmdb.appsystemname"),0));
        jsonList.add(new PatternVo("appSystemAbbrName", "input", ApiParamType.STRING, 0, $.t("term.cmdb.appsystemabbrname"),0));
        jsonList.add(new PatternVo("appModuleId", "input", ApiParamType.LONG, 0, $.t("term.cmdb.appmoduleid"),0));
        jsonList.add(new PatternVo("appModuleName", "input", ApiParamType.STRING, 0, $.t("term.cmdb.appmodulename"),0));
        jsonList.add(new PatternVo("appModuleAbbrName", "input", ApiParamType.STRING, 0, $.t("term.cmdb.appmoduleabbrname"),0));
        jsonList.add(new PatternVo("envName", "input", ApiParamType.STRING, 0, $.t("term.cmdb.envname"),0));
        jsonList.add(new PatternVo("buildNo", "input", ApiParamType.INTEGER, 0, $.t("nmdih.deploytriggerintegrationhandler.runtime.pattern.buildno"),0));
        jsonList.add(new PatternVo("scenarioName", "input", ApiParamType.STRING, 0, $.t("term.autoexec.scenario")));
        jsonList.add(new PatternVo("targetEnvName", "input", ApiParamType.STRING, 0, $.t("nmdih.deploytriggerintegrationhandler.runtime.pattern.targetenvname")));
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
