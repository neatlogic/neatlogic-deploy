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
package neatlogic.module.deploy.notify.handler;

import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.INotifyPolicyHandlerGroup;
import neatlogic.framework.notify.core.NotifyHandlerType;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;
import neatlogic.framework.notify.dto.NotifyTriggerTemplateVo;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.util.I18nUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/12/29 11:23
 */

@Component
public class DeployJobNotifyPolicyHandler extends NotifyPolicyHandlerBase {
    @Override
    public String getName() {
        return "handler.notify.deploy.job";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return DEPLOY_MODIFY.class.getSimpleName();
    }

    @Override
    public INotifyPolicyHandlerGroup getGroup() {
        return null;
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (DeployJobNotifyTriggerType triggerType : DeployJobNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType.getTrigger(), I18nUtils.getMessage(triggerType.getText()), I18nUtils.getMessage(triggerType.getDescription())));
        }
        return returnList;
    }

    @Override
    protected List<NotifyTriggerTemplateVo> myNotifyTriggerTemplateList(NotifyHandlerType type) {
        return new ArrayList<>();
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        List<ConditionParamVo> notifyPolicyParamList = new ArrayList<>();
        for (DeployJobNotifyParam param : DeployJobNotifyParam.values()) {
            ConditionParamVo paramVo = new ConditionParamVo();
            paramVo.setName(param.getValue());
            paramVo.setLabel(param.getText());
            paramVo.setParamType(param.getParamType().getName());
            paramVo.setParamTypeName(param.getParamType().getText());
            paramVo.setFreemarkerTemplate(param.getFreemarkerTemplate());
            paramVo.setIsEditable(0);
            notifyPolicyParamList.add(paramVo);
        }
        return notifyPolicyParamList;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }
}
