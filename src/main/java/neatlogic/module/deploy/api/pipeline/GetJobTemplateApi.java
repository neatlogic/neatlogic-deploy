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

package neatlogic.module.deploy.api.pipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetJobTemplateApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Override
    public String getName() {
        return "nmdap.getjobtemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/jobtemplate/get";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmdap.getjobtemplateapi.input.param.desc.id")})
    @Output({@Param(explode = PipelineJobTemplateVo.class)})
    @Description(desc = "nmdap.getjobtemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return deployPipelineMapper.getJobTemplateById(jsonObj.getLong("id"));
    }

}
