/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 查询组合工具详情接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployPipelineTemplateGetApi extends PrivateApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/get";
    }

    @Override
    public String getName() {
        return "查询流水线模板详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = DeployPipelineTemplateVo.class, desc = "流水线模板详情")
    })
    @Description(desc = "查询流水线模板详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        DeployPipelineTemplateVo deployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
        if (deployPipelineTemplateVo == null) {
            throw new DeployPinelineTemplateNotFoundException(id);
        }
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        autoexecServiceCrossoverService.updateAutoexecCombopConfig(deployPipelineTemplateVo.getConfig());
        return deployPipelineTemplateVo;
    }
}
