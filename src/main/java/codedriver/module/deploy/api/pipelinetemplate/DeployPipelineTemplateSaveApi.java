/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNameRepeatException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNotFoundException;
import codedriver.framework.deploy.exception.DeployPinelineTemplatePhaseNameRepeatException;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 保存组合工具基本信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployPipelineTemplateSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/save";
    }

    @Override
    public String getName() {
        return "保存流水线模板基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
//            @Param(name = "uk", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "唯一名"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
//            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
//            @Param(name = "owner", type = ApiParamType.STRING, minLength = 37, maxLength = 37, desc = "维护人"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存流水线模板基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployPipelineTemplateVo deployPipelineTemplateVo = jsonObj.toJavaObject(DeployPipelineTemplateVo.class);
        if (deployPipelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPipelineTemplateVo) != null) {
            throw new DeployPinelineTemplateNameRepeatException(deployPipelineTemplateVo.getName());
        }
        if (autoexecTypeMapper.checkTypeIsExistsById(deployPipelineTemplateVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(deployPipelineTemplateVo.getTypeId());
        }
        Long id = jsonObj.getLong("id");
        if (id == null) {
            deployPipelineTemplateVo.setConfig("{}");
            deployPipelineTemplateMapper.insertPinelineTemplate(deployPipelineTemplateVo);
        } else {
            DeployPipelineTemplateVo oldDeployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
            if (oldDeployPipelineTemplateVo == null) {
                throw new DeployPinelineTemplateNotFoundException(id);
            }
            AutoexecCombopConfigVo config = deployPipelineTemplateVo.getConfig();
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            List<String> nameList = new ArrayList<>();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                String name = autoexecCombopPhaseVo.getName();
                if (nameList.contains(name)) {
                    throw new DeployPinelineTemplatePhaseNameRepeatException(name);
                }
                nameList.add(name);
            }
            deployPipelineTemplateMapper.updatePinelineTemplateById(deployPipelineTemplateVo);
        }

        return deployPipelineTemplateVo.getId();
    }

    public IValid name() {
        return jsonObj -> {
            DeployPipelineTemplateVo deployPipelineTemplateVo = jsonObj.toJavaObject(DeployPipelineTemplateVo.class);
            if (deployPipelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPipelineTemplateVo) != null) {
                return new FieldValidResultVo(new DeployPinelineTemplateNameRepeatException(deployPipelineTemplateVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
