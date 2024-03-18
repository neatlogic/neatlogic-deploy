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
package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
