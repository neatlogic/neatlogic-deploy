/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.dto.pipeline.PipelineSearchVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
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
public class SearchPipelineApi extends PrivateApiComponentBase {
    @Resource
    private PipelineService pipelineService;

    @Override
    public String getName() {
        return "查询超级流水线";
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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "type", type = ApiParamType.ENUM, member = PipelineType.class, desc = "类型"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "needVerifyAuth", type = ApiParamType.INTEGER, desc = "是否需要验证权限"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = PipelineVo[].class, desc = "超级流水线列表")
    })
    @Description(desc = "查询超级流水线接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        PipelineSearchVo searchVo = JSONObject.toJavaObject(jsonObj, PipelineSearchVo.class);
        return TableResultUtil.getResult(pipelineService.searchPipeline(searchVo), searchVo);
    }

}
