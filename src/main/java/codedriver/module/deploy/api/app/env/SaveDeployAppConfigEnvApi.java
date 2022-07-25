package codedriver.module.deploy.api.app.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存发布应用配置的应用系统环境";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "环境id列表"),
    })
    @Output({
    })
    @Description(desc = "保存发布应用配置的应用系统环境")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }
        JSONArray envIdArray = paramObj.getJSONArray("envIdList");
        List<Long> envIdList = null;
        if (CollectionUtils.isNotEmpty(envIdArray)) {
            envIdList = envIdArray.toJavaList(Long.class);
        }
        for (Long envId : envIdList) {
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(envId) == null) {
                throw new CiEntityNotFoundException(envId);
            }
        }

        deployAppConfigMapper.insertAppConfigEnv(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), envIdList);
        return null;
    }
}
