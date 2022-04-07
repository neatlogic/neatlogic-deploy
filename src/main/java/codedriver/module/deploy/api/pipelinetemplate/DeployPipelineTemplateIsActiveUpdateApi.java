/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 更新组合工具状态接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployPipelineTemplateIsActiveUpdateApi extends PrivateApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/isactive/update";
    }

    @Override
    public String getName() {
        return "更新流水线模板状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.INTEGER, desc = "更新后的状态")
    })
    @Description(desc = "更新流水线模板状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer isActive = deployPipelineTemplateMapper.getPinelineTemplateIsActiveByIdForUpdate(id);
        if (isActive == null) {
            throw new DeployPinelineTemplateNotFoundException(id);
        }
        DeployPipelineTemplateVo deployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
        /** 如果是激活组合工具，则需要校验该组合工具配置正确 **/
//        if (isActive == 0) {
//            autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopVo, false);
//        }
        deployPipelineTemplateVo.setLcu(UserContext.get().getUserUuid(true));
        deployPipelineTemplateMapper.updatePinelineTemplateIsActiveById(deployPipelineTemplateVo);
        return (1 - isActive);
    }
}
