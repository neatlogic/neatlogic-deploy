package codedriver.module.deploy.api.version;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionEnvListApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取版本的环境列表";
    }

    @Override
    public String getToken() {
        return "deploy/version/env/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id"),
    })
    @Output({
            @Param(name = "Return", explode = AppEnvironmentVo[].class),
    })
    @Description(desc = "获取版本的环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long versionId = paramObj.getLong("versionId");
        DeployVersionVo version = deployVersionMapper.getDeployVersionBaseInfoById(versionId);
        if (version == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        return deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndModuleId(version.getAppSystemId(), version.getAppModuleId());
    }
}
