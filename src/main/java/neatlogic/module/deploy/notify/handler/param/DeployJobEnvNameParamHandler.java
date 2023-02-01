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
package neatlogic.module.deploy.notify.handler.param;

import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

/**
 * @author longrf
 * @date 2022/12/29 14:22
 */
@Component
public class DeployJobEnvNameParamHandler extends DeployJobNotifyParamHandlerBase {

    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return deployJobVo.getEnvName();
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.ENVNAME.getValue();
    }
}
