package neatlogic.module.deploy.api.appconfig.system;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppSystemFavoriteApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaas.savedeployappconfigappsystemfavoriteapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/system/favorite/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaas.savedeployappconfigappsystemfavoriteapi.input.param.desc.appsystemid"),
            @Param(name = "isFavorite", type = ApiParamType.INTEGER, isRequired = true, desc = "nmdaas.savedeployappconfigappsystemfavoriteapi.input.param.desc.isfavorite")
    })
    @Output({
    })
    @Description(desc = "nmdaas.savedeployappconfigappsystemfavoriteapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (Objects.equals(paramObj.getInteger("isFavorite"), 1)) {
            deployAppConfigMapper.insertAppConfigSystemFavorite(paramObj.getLong("appSystemId"), UserContext.get().getUserUuid());
        } else if (Objects.equals(paramObj.getInteger("isFavorite"), 0)) {
            deployAppConfigMapper.deleteAppConfigSystemFavoriteByAppSystemIdAndUserUuid(paramObj.getLong("appSystemId"), UserContext.get().getUserUuid());
        }
        return null;
    }
}
