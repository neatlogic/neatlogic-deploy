package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppSystemFavoriteApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "收藏发布应用配置的应用";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "isFavorite", type = ApiParamType.INTEGER, isRequired = true, desc = "是否被收藏（1：收藏，0：取消收藏）")
    })
    @Output({
    })
    @Description(desc = "收藏发布应用配置的应用")
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
