package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/7/11 6:08 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployJobModuleApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "根据系统环境查询一键发布页面的模块列表";
    }

    @Override
    public String getToken() {
        return "deploy/job/module/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {


        int count = deployAppConfigMapper.getAppModuleCountBySystemIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("envId"), TenantContext.get().getDataDbName());
        if (count > 0) {
            returnAppModuleVoList = deployAppConfigMapper.getAppModuleListBySystemIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("envId"), TenantContext.get().getDataDbName());
        }

        return returnAppModuleVoList;
    }
}



