package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/7/1 11:25 上午
 */
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployAppConfigEnvDBConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

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
            @Param(name = "DBAlias", type = ApiParamType.STRING, isRequired = true, desc = "数据库别名"),
            @Param(name = "DBResourceId", type = ApiParamType.LONG, isRequired = true, desc = "数据库资产id"),
            @Param(name = "accountList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "执行用户列表"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "高级设置")
    })
    @Description(desc = "保存发布应用配置DB配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

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

        DeployAppConfigEnvDBConfigVo dbConfigVo = paramObj.toJavaObject(DeployAppConfigEnvDBConfigVo.class);
        deployAppConfigMapper.insertAppConfigEnvDBConfig(dbConfigVo);
        deployAppConfigMapper.insertAppConfigEnvDBConfigAccount(dbConfigVo.getId(), dbConfigVo.getAccountList());

        return null;
    }
}
