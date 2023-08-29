package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionTheadApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.getdeployversiontheadapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/thead/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({

    })
    @Description(desc = "nmdav.getdeployversiontheadapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return deployVersionMapper.getDeployVersionTheadByUserUuid(UserContext.get().getUserUuid(true));
    }
}
