/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNameRepeatException;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 复制组合工具模板接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployPipelineTemplateCopyApi extends PrivateApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/copy";
    }

    @Override
    public String getName() {
        return "复制流水线模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "被复制的流水线模板id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "新流水线模板名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "复制流水线模板")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        DeployPipelineTemplateVo deployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
        if (deployPipelineTemplateVo == null) {
            throw new DeployPinelineTemplateNotFoundException(id);
        }
        Long typeId = jsonObj.getLong("typeId");
        if (autoexecTypeMapper.checkTypeIsExistsById(typeId) == 0) {
            throw new AutoexecTypeNotFoundException(typeId);
        }
        deployPipelineTemplateVo.setTypeId(typeId);
        String name = jsonObj.getString("name");
        deployPipelineTemplateVo.setName(name);
        deployPipelineTemplateVo.setId(null);
        if (deployPipelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPipelineTemplateVo) != null) {
            throw new DeployPinelineTemplateNameRepeatException(deployPipelineTemplateVo.getName());
        }
        String userUuid = UserContext.get().getUserUuid(true);
        deployPipelineTemplateVo.setFcu(userUuid);
        deployPipelineTemplateVo.setDescription(jsonObj.getString("description"));
        deployPipelineTemplateMapper.insertPinelineTemplate(deployPipelineTemplateVo);
        return deployPipelineTemplateVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            String name = jsonObj.getString("name");
            DeployPipelineTemplateVo deployPipelineTemplateVo = new DeployPipelineTemplateVo();
            deployPipelineTemplateVo.setName(name);
            if (deployPipelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPipelineTemplateVo) != null) {
                return new FieldValidResultVo(new DeployPinelineTemplateNameRepeatException(deployPipelineTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
