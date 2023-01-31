/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
