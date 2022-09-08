package codedriver.module.deploy.api.ci;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiGitlabEventApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getName() {
        return "gitlab webhook回调api";
    }

    @Override
    public String getToken() {
        return "deploy/ci/gitlab/event/callback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }

    @Input({
            @Param(name = "ciId", desc = "持续集成配置id", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "gitlab webhook回调api")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return null;
    }

}
