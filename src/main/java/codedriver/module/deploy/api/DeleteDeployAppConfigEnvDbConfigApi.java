package codedriver.module.deploy.api;

import codedriver.framework.common.constvalue.ApiParamType;
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
 * @date 2022/7/7 10:49 上午
 */
@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployAppConfigEnvDbConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "删除发布应用配置的DB配置";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")
    })
    @Description(desc = "删除发布应用配置的DB配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");

        deployAppConfigMapper.deleteAppConfigDBConfigById(id);
        deployAppConfigMapper.deleteAppConfigDBConfigAccountByDBConfigId(id);
        return null;
    }
}
