/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.webhook;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployJobWebhookMapper;
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
