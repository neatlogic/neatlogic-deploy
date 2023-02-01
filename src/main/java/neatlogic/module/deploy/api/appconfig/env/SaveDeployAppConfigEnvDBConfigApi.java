package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
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
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "保存发布应用配置DB配置";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "dbSchema", type = ApiParamType.REGEX, isRequired = true, rule = RegexUtils.DB_SCHEMA, desc = "数据库schema"),
            @Param(name = "dbResourceId", type = ApiParamType.LONG, isRequired = true, desc = "数据库资产id"),
            @Param(name = "accountId", type = ApiParamType.LONG, isRequired = true, desc = "执行用户id"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "高级设置")
    })
    @Description(desc = "保存发布应用配置DB配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {


        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("envId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("envId"));
        }
        CiEntityVo DBCiEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("dbResourceId"));
        if (DBCiEntityVo == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("dbResourceId"));
        }

        //保存DB发布配置
        DeployAppConfigEnvDBConfigVo dbConfigVo = paramObj.toJavaObject(DeployAppConfigEnvDBConfigVo.class);
        if (deployAppConfigMapper.checkDeployAppConfigEnvDBSchemaIsRepeat(dbConfigVo) > 0) {
            throw new DeployAppConfigEnvDBSchemaNameRepeatException(dbConfigVo.getDbSchema());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            deployAppConfigMapper.updateDeployAppConfigEnvDBConfig(dbConfigVo);
        } else {
            deployAppConfigMapper.insertAppConfigEnvDBConfig(dbConfigVo);
        }

        //保存DB配置项系统模块环境信息
        //添加环境属性、模块关系
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo DBCiEntityInfo = ciEntityService.getCiEntityById(DBCiEntityVo.getCiId(), DBCiEntityVo.getId());
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(DBCiEntityInfo);
        ciEntityTransactionVo.setAttrEntityData(DBCiEntityInfo.getAttrEntityData());
        deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, DBCiEntityInfo.getCiId(), paramObj, Collections.singletonList("app_environment"), Collections.singletonList("APPComponent"));

        //保存
        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);

        return null;
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
