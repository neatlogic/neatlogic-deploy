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
public class DeleteDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "删除发布应用配置应用";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/system/delete";
    }


    @Input({@Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Description(desc = "删除发布应用配置应用")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }

        //如果是发布新增的环境，需要删除
        if (deployAppConfigMapper.getAppConfigEnvByAppSystemId(paramObj.getLong("appSystemId")) > 0) {
            deployAppConfigMapper.deleteAppConfigEnvByAppSystemId(paramObj.getLong("appSystemId"));
        }
        //删除config表
        deployAppConfigMapper.deleteAppConfigByAppSystemId(paramObj.getLong("appSystemId"));
        deployAppConfigMapper.deleteAppConfigAuthorityByAppSystemId(paramObj.getLong("appSystemId"));
        deployAppConfigMapper.deleteAppConfigDraftByAppSystemId(paramObj.getLong("appSystemId"));
        deployAppConfigMapper.deleteAppConfigOverrideByAppSystemId(paramObj.getLong("appSystemId"));
        return null;
    }
}
