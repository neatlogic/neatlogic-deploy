/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.deploy.api.webhook;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployJobWebhookAuditApi extends PrivateApiComponentBase {
    @Resource
    DeployJobWebhookMapper webhookMapper;

    @Override
    public String getName() {
        return "查询发布作业触发器记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "webhookId", type = ApiParamType.LONG, isRequired = true, desc = "触发器id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = DeployJobWebhookAuditVo[].class, desc = "作业触发器记录列表"),})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployJobWebhookAuditVo deployJobWebhookAuditVo = paramObj.toJavaObject(DeployJobWebhookAuditVo.class);
        List<DeployJobWebhookAuditVo> webhookAuditVoList = new ArrayList<>();
        int count = webhookMapper.getWebhookAuditCount(deployJobWebhookAuditVo);
        if (count > 0) {
            deployJobWebhookAuditVo.setRowNum(count);
            webhookAuditVoList = webhookMapper.searchWebhookAudit(deployJobWebhookAuditVo);
            return TableResultUtil.getResult(webhookAuditVoList, deployJobWebhookAuditVo);
        }
        return webhookAuditVoList;
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/audit/search";
    }
}
