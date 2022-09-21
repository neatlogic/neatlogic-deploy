/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.webhook;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookVo;
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
public class SearchDeployJobWebhookApi extends PrivateApiComponentBase {
    @Resource
    DeployJobWebhookMapper webhookMapper;

    @Override
    public String getName() {
        return "查询发布作业触发器";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = DeployScheduleVo[].class, desc = "定时作业列表"),})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployJobWebhookVo deployJobWebhookVo = paramObj.toJavaObject(DeployJobWebhookVo.class);
        List<DeployJobWebhookVo> webhookVoList = new ArrayList<>();
        int count = webhookMapper.getWebhookCount(deployJobWebhookVo);
        if (count > 0) {
            deployJobWebhookVo.setRowNum(count);
            webhookVoList = webhookMapper.searchWebhook(deployJobWebhookVo);
            return TableResultUtil.getResult(webhookVoList, deployJobWebhookVo);
        }
        return webhookVoList;
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/search";
    }
}
