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

package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
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
        return "nmdaj.getdeploygroupjoblistapi.getname";
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
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "nmdaj.getdeploygroupjoblistapi.input.param.desc.idlist"),
    })
    @Output({@Param(type = ApiParamType.JSONARRAY, explode = LaneGroupVo[].class)})
    @Description(desc = "nmdaj.getdeploygroupjoblistapi.getname")
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
