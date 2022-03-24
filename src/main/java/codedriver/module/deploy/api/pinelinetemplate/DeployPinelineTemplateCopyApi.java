/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pinelinetemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.deploy.dto.pinelinetemplate.DeployPinelineTemplateVo;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNameRepeatException;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPinelineTemplateMapper;
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
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployPinelineTemplateCopyApi extends PrivateApiComponentBase {

    @Resource
    private DeployPinelineTemplateMapper deployPinelineTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "deploy/pinelinetemplate/copy";
    }

    @Override
    public String getName() {
        return "复制组合工具模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "被复制的组合工具模板id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "新组合工具模板名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "复制组合工具模板")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        DeployPinelineTemplateVo deployPinelineTemplateVo = deployPinelineTemplateMapper.getPinelineTemplateById(id);
        if (deployPinelineTemplateVo == null) {
            throw new DeployPinelineTemplateNotFoundException(id);
        }
        Long typeId = jsonObj.getLong("typeId");
        if (autoexecTypeMapper.checkTypeIsExistsById(typeId) == 0) {
            throw new AutoexecTypeNotFoundException(typeId);
        }
        deployPinelineTemplateVo.setTypeId(typeId);
        String name = jsonObj.getString("name");
        deployPinelineTemplateVo.setName(name);
        deployPinelineTemplateVo.setId(null);
        if (deployPinelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPinelineTemplateVo) != null) {
            throw new DeployPinelineTemplateNameRepeatException(deployPinelineTemplateVo.getName());
        }
        String userUuid = UserContext.get().getUserUuid(true);
        deployPinelineTemplateVo.setFcu(userUuid);
        deployPinelineTemplateVo.setOperationType(CombopOperationType.COMBOP.getValue());
        deployPinelineTemplateVo.setDescription(jsonObj.getString("description"));
        deployPinelineTemplateMapper.insertPinelineTemplate(deployPinelineTemplateVo);
        return deployPinelineTemplateVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            String name = jsonObj.getString("name");
            DeployPinelineTemplateVo deployPinelineTemplateVo = new DeployPinelineTemplateVo();
            deployPinelineTemplateVo.setName(name);
            if (deployPinelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPinelineTemplateVo) != null) {
                return new FieldValidResultVo(new DeployPinelineTemplateNameRepeatException(deployPinelineTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
