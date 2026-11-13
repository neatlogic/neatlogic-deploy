package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployEnvironmentWriter;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployAppConfigEnvCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private DeployEnvironmentWriter writer;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvdbapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/cientity/save";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "term.cmdb.ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.port"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name"),
//            @Param(name = "attrList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "common.attributelist"),
//            @Param(name = "relList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.rellist"),
//            @Param(name = "globalAttrList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.globalattrlist"),
    })
    @Output({
            @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid")
    })
    @Description(desc = "nmdaae.savedeployappconfigenvdbapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return writer.saveResource(paramObj);
    }
}
