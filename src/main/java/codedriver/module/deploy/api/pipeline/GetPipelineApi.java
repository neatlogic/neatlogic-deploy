/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPipelineApi extends PrivateApiComponentBase {
    @Resource
    private PipelineMapper pipelineMapper;

    @Override
    public String getName() {
        return "获取超级流水线详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/get";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)
    })
    @Output({@Param(explode = PipelineVo.class)})
    @Description(desc = "获取超级流水线详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return pipelineMapper.getPipelineById(jsonObj.getLong("id"));
    }

}
