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

package neatlogic.module.deploy.api.pipeline;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.service.PipelineService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchJobTemplateApi extends PrivateApiComponentBase {
    @Resource
    private PipelineService pipelineService;

    @Override
    public String getName() {
        return "搜索流水线作业模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/jobtemplate/search";
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "excludeIdList", type = ApiParamType.JSONARRAY, desc = "排除id列表"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id")})
    @Output({@Param(explode = BasePageVo.class)})
    @Description(desc = "搜索流水线作业模板接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        PipelineJobTemplateVo pipelineJobTemplateVo = JSONObject.toJavaObject(jsonObj, PipelineJobTemplateVo.class);
        return TableResultUtil.getResult(pipelineService.searchPipelineJobTemplate(pipelineJobTemplateVo));
    }

}
