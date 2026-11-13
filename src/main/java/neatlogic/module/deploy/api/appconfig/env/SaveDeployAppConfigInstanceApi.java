package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployEnvironmentWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/27 6:03 下午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    private DeployEnvironmentWriter writer;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfiginstanceapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/save";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "instanceIdList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfiginstanceapi.input.param.desc.instanceidlist"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfiginstanceapi.input.param.desc.id"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "term.cmdb.ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "nmdaae.savedeployappconfiginstanceapi.input.param.desc.port"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name"),
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "term.cmdb.maintenancewindow")
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        writer.saveInstance(paramObj);
        return null;
    }
}
