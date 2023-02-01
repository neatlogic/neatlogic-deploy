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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployGroupJobListApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;


    @Override
    public String getName() {
        return "根据指定id获取发布作业组列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/group/list";
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找作业刷新状态"),
    })
    @Output({@Param(type = ApiParamType.JSONARRAY, explode = LaneGroupVo[].class)})
    @Description(desc = "根据指定id获取发布作业组列表接口，用于刷新作业状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<Long> idList = new ArrayList<>();
        JSONArray pIdList = jsonObj.getJSONArray("idList");
        for (int i = 0; i < pIdList.size(); i++) {
            idList.add(pIdList.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            return deployJobMapper.getDeployJobGroupByJobIdList(idList);
        }
        return null;
    }

}
