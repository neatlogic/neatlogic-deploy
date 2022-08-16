/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.service.PipelineService;
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
