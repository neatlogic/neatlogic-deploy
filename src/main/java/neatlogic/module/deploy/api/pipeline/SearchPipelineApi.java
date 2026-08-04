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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployTenantConfig;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.dto.pipeline.PipelineSearchVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.service.PipelineService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchPipelineApi extends PrivateApiComponentBase {
    @Resource
    private PipelineService pipelineService;

    @Override
    public String getName() {
        return "nmdap.searchpipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/search";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdap.searchpipelineapi.input.param.desc.keyword"),
            @Param(name = "type", type = ApiParamType.ENUM, member = PipelineType.class, desc = "nmdap.searchpipelineapi.input.param.desc.type"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "nmdap.searchpipelineapi.input.param.desc.appsystemid"),
            @Param(name = "needVerifyAuth", type = ApiParamType.INTEGER, desc = "nmdap.searchpipelineapi.input.param.desc.needverifyauth"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "nmdap.searchpipelineapi.input.param.desc.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "nmdap.searchpipelineapi.input.param.desc.pagesize")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = PipelineVo[].class, desc = "nmdap.searchpipelineapi.output.param.desc.tbodylist")
    })
    @Description(desc = "nmdap.searchpipelineapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        PipelineSearchVo searchVo = JSON.toJavaObject(jsonObj, PipelineSearchVo.class);
        JSONObject result = TableResultUtil.getResult(pipelineService.searchPipeline(searchVo), searchVo);
        try {
            result.put("isNeedDefaultVersion", Integer.valueOf(ConfigManager.getConfig(DeployTenantConfig.IS_PIPELINE_NEED_DEFAULT_VERSION)));
        }catch (Exception e){
            result.put("isNeedDefaultVersion",0);
        }
        return result;
    }

}
