package codedriver.module.deploy.api.param;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.exception.param.DeployGlobalParamIsNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 2:10 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeployGlobalParamDeleteApi extends PrivateApiComponentBase {

    @Resource
    DeployGlobalParamMapper deployGlobalParamMapper;

    @Override
    public String getName() {
        return "删除发布全局参数";
    }

    @Override
    public String getToken() {
        return "deploy/global/param/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Description(desc = "删除发布全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        if (deployGlobalParamMapper.checkGlobalParamIsExistsById(paramId) == 0) {
            throw new DeployGlobalParamIsNotFoundException(paramId);
        }
        deployGlobalParamMapper.deleteGlobalParamById(paramId);
        return null;
    }
}
