package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.exception.DeployCiNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployActiveStatusApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "激活或禁用持续集成配置";
    }

    @Override
    public String getToken() {
        return "deploy/ci/activestatus/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "isActive", desc = "是否激活", rule = "0,1", type = ApiParamType.ENUM, isRequired = true),
    })
    @Description(desc = "激活或禁用持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployCiVo ci = deployCiMapper.getDeployCiById(id);
        if (ci == null) {
            throw new DeployCiNotFoundException(id);
        }
        deployAppAuthorityService.checkOperationAuth(ci.getAppSystemId(), DeployAppConfigAction.EDIT);
        deployCiMapper.updateDeployActiveStatus(id, paramObj.getInteger("isActive"));
        return null;
    }

}
