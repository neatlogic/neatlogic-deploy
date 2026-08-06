/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.api.webhook;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
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
        return "nmdaw.searchdeployjobwebhookapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdaw.searchdeployjobwebhookapi.input.param.desc.keyword"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "nmdaw.searchdeployjobwebhookapi.input.param.desc.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "nmdaw.searchdeployjobwebhookapi.input.param.desc.pagesize")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = DeployScheduleVo[].class, desc = "nmdaw.searchdeployjobwebhookapi.output.param.desc.tbodylist"),})
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
