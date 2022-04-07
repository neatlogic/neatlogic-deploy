/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 删除组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployPipelineTemplateDeleteApi extends PrivateApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/delete";
    }

    @Override
    public String getName() {
        return "删除流水线模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Description(desc = "删除流水线模板")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        DeployPipelineTemplateVo deployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
        if (deployPipelineTemplateVo != null) {
            deployPipelineTemplateMapper.deletePinelineTemplateById(id);
//            DependencyManager.delete(MatrixAutoexecCombopParamDependencyHandler.class, id);
        }
        return null;
    }
}
