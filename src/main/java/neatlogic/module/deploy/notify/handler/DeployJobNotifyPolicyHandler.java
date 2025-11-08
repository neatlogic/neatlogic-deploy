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
package neatlogic.module.deploy.notify.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobGroupSearch;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
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
        return "term.deploy.deployjob";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return DEPLOY_MODIFY.class.getSimpleName();
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (DeployJobNotifyTriggerType triggerType : DeployJobNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType));
        }
        return returnList;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        List<ConditionParamVo> notifyPolicyParamList = new ArrayList<>();
        for (DeployJobNotifyParam param : DeployJobNotifyParam.values()) {
            notifyPolicyParamList.add(createConditionParam(param));
        }
        return notifyPolicyParamList;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {
        List<String> groupList = JSON.parseArray(config.getJSONArray("groupList").toJSONString(), String.class);
        groupList.add(JobGroupSearch.JOBUSERTYPE.getValue());
        config.put("groupList", groupList);
    }
}
