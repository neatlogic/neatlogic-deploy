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
package neatlogic.module.deploy.notify.handler.param;

import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author longrf
 * @date 2022/12/29 14:18
 */
@Component
public class DeployJobAppModuleNameAndAbbrNameParamHandler extends DeployJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return deployJobVo.getAppModuleAbbrName() + (StringUtils.isNotEmpty(deployJobVo.getAppModuleName()) ? "(" + deployJobVo.getAppModuleName() + ")" : "");
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.APPMODULENAMEANDABBRNAME.getValue();
    }
}
