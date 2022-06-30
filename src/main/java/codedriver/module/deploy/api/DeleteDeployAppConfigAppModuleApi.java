package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/27 11:22 上午
 */
@Service
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "删除发布应用配置模块";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/module/delete";
    }


    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id")
    })
    @Description(desc = "删除发布应用配置模块")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }

        //如果是发布新增的环境，需要删除
        if (deployAppConfigMapper.getAppConfigEnvByAppSystemIdAndAppModuleId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId")) > 0) {
            deployAppConfigMapper.deleteAppConfigEnvByAppSystemIdAndAppModuleId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"));
        }
        //删除config表
        deployAppConfigMapper.deleteAppConfigByAppSystemIdAndAppModuleId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"));
        deployAppConfigMapper.deleteAppConfigDraftByAppSystemIdAndAppModuleId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"));
        deployAppConfigMapper.deleteAppConfigOverrideByAppSystemIdAndAppModuleId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"));
        return null;
    }
}
