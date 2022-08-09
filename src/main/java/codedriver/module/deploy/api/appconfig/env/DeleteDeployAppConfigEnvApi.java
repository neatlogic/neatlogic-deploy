package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import codedriver.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployAppConfigEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "删除发布应用配置的应用系统环境";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
    })
    @Output({
    })
    @Description(desc = "删除发布应用配置的应用系统环境")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        //删除配置
        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        deployAppConfigService.deleteAppConfig(deployAppConfigVo);

        //删除发布存的环境
        if (deployAppConfigMapper.getAppConfigEnv(deployAppConfigVo) > 0) {
            deployAppConfigMapper.deleteAppConfigEnv(deployAppConfigVo);
        }
        return null;
    }
}
