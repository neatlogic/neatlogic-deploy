package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
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
        return "nmdaae.deletedeployappconfigenvapi.getname";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvapi.input.param.desc.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvapi.input.param.desc.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.deletedeployappconfigenvapi.input.param.desc.envid"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.deletedeployappconfigenvapi.getname")
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
