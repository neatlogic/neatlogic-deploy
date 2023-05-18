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
package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dependency.handler.NotifyPolicyDeployJobDependencyHandler;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.notify.handler.DeployJobNotifyPolicyHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author longrf
 * @date 2022/12/29 17:11
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployJobNotifyPolicyApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "保存发布作业通知策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "policyId", type = ApiParamType.LONG, desc = "通知策略id"),
            @Param(name = "isCustom", type = ApiParamType.ENUM, rule = "0,1", desc = "通知策略id"),
            @Param(name = "paramMappingList", type = ApiParamType.JSONARRAY, desc = "参数映射列表"),
            @Param(name = "excludeTriggerList", type = ApiParamType.JSONARRAY, desc = "排除的触发点列表")
    })
    @Output({
    })
    @Description(desc = "保存发布作业通知策略")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        DependencyManager.delete(NotifyPolicyDeployJobDependencyHandler.class, appSystemId);
        Long policyId = paramObj.getLong("policyId");
        if (policyId != null) {
            INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
            InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = paramObj.toJavaObject(InvokeNotifyPolicyConfigVo.class);
            invokeNotifyPolicyConfigVo.setHandler(DeployJobNotifyPolicyHandler.class.getName());
            invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
            JSONArray to = new JSONArray();
            to.add(appSystemId);
            to.add(JSONObject.toJSONString(invokeNotifyPolicyConfigVo));
            DependencyManager.insert(NotifyPolicyDeployJobDependencyHandler.class, policyId, to);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/job/notify/policy/save";
    }
}
