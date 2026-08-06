package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionLastUnitTestApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.getdeployversionlastunittestapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/lastunittest/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", desc = "common.versionid", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "nmdav.getdeployversionlastunittestapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return deployVersionMapper.getDeployVersionUnitTestListByVersionIdWithLimit(paramObj.getLong("versionId"), 1);
    }
}
