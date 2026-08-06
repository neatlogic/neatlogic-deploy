package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppConfigService;
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
    DeployAppConfigService deployAppConfigService;

    @Override
    public String getName() {
        return "nmdav.getdeployversionenvlistapi.getname";
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
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "nmdav.getdeployversionenvlistapi.input.param.desc.versionid"),
    })
    @Output({
            @Param(name = "Return", explode = AppEnvironmentVo[].class),
    })
    @Description(desc = "nmdav.getdeployversionenvlistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long versionId = paramObj.getLong("versionId");
        DeployVersionVo version = deployVersionMapper.getDeployVersionBaseInfoById(versionId);
        if (version == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        return deployAppConfigService.getDeployAppModuleEnvListByAppSystemIdAndModuleId(version.getAppSystemId(), version.getAppModuleId());
    }
}
