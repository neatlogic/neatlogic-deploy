package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvDBSchemaNameRepeatException;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployEnvironmentWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/7/1 11:25 上午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployAppConfigEnvDBConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployEnvironmentWriter writer;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvdbconfigapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/save";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "dbSchema", type = ApiParamType.REGEX, isRequired = true, rule = RegexUtils.DB_SCHEMA, desc = "nmdaae.savedeployappconfigenvdbconfigapi.input.param.desc.dbschema"),
            @Param(name = "dbResourceId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.savedeployappconfigenvdbconfigapi.input.param.desc.dbresourceid"),
            @Param(name = "accountId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.savedeployappconfigenvdbconfigapi.input.param.desc.accountid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "nmdaae.savedeployappconfigenvdbconfigapi.input.param.desc.config")
    })
    @Description(desc = "nmdaae.savedeployappconfigenvdbconfigapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return writer.saveSchema(paramObj);
    }

    public IValid dbSchema() {
        return value -> {
            DeployAppConfigEnvDBConfigVo configVo = JSON.toJavaObject(value, DeployAppConfigEnvDBConfigVo.class);
            if (deployAppConfigMapper.checkDeployAppConfigEnvDBSchemaIsRepeat(configVo) > 0) {
                return new FieldValidResultVo(new DeployAppConfigEnvDBSchemaNameRepeatException(configVo.getDbSchema()));
            }
            return new FieldValidResultVo();
        };
    }
}
