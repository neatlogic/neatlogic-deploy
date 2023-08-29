package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionTheadVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployVersionTheadApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.savedeployversiontheadapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/thead/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "config", desc = "nmdav.savedeployversiontheadapi.input.param.desc.config", isRequired = true, type = ApiParamType.STRING),
    })
    @Description(desc = "nmdav.savedeployversiontheadapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployVersionTheadVo deployVersionTheadVo = JSONObject.toJavaObject(paramObj,DeployVersionTheadVo.class);
        deployVersionMapper.insertDeployVersionThead(deployVersionTheadVo);
        return null;
    }
}
