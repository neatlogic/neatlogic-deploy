/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.pipeline.PipelineGroupVo;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineLaneVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import codedriver.module.deploy.service.PipelineService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPipelineApi extends PrivateApiComponentBase {
    @Resource
    private PipelineMapper pipelineMapper;

    @Resource
    private PipelineService pipelineService;

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
        PipelineVo pipelineVo = pipelineMapper.getPipelineById(jsonObj.getLong("id"));
        // 补充系统模块的名称、简称
        List<PipelineJobTemplateVo> pipelineJobTemplateVoList = new ArrayList<>();
        for (PipelineLaneVo pipelineLaneVo : pipelineVo.getLaneList()) {
            for (PipelineGroupVo pipelineGroupVo : pipelineLaneVo.getGroupList()) {
                pipelineJobTemplateVoList.addAll(pipelineGroupVo.getJobTemplateList());
            }
        }
        pipelineService.setDeployPipelineJobTemplateAppSystemNameAndAppModuleName(pipelineJobTemplateVoList);
        return pipelineVo;
    }

}
