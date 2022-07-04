package codedriver.module.deploy.api;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.integration.dto.table.ColumnVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/7/1 2:55 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigEnvDBConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取发布应用配置DB配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "数据库id")
    })
    @Output({
            @Param(name = "tbodyList", explode = ColumnVo[].class, desc = "矩阵属性集合")
    })
    @Description(desc = "获取发布应用配置DB配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        return deployAppConfigMapper.getAppConfigEnvDBConfigByAppSystemIdAndAppModuleIdAndEnvIdAndResourceId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), paramObj.getLong("resourceId"));
    }
}
