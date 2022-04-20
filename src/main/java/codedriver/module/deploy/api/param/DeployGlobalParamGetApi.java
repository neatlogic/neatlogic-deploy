package codedriver.module.deploy.api.param;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.ParamValueType;
import codedriver.framework.deploy.dto.param.DeployGlobalParamVo;
import codedriver.framework.deploy.exception.param.DeployGlobalParamIsNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/19 9:49 上午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployGlobalParamGetApi extends PrivateApiComponentBase {

    @Resource
    DeployGlobalParamMapper deployGlobalParamMapper;

    @Override
    public String getName() {
        return "获取发布全局参数";
    }

    @Override
    public String getToken() {
        return "deploy/global/param/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Description(desc = "获取发布全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        DeployGlobalParamVo globalParamVo = deployGlobalParamMapper.getGlobalParamById(paramId);
        if (globalParamVo == null) {
            throw new DeployGlobalParamIsNotFoundException(paramId);
        }
        if (StringUtils.equals(ParamValueType.PASSWORD.getValue(), globalParamVo.getValueType()) && StringUtils.isNotBlank(globalParamVo.getValue()) && globalParamVo.getValue().startsWith(CiphertextPrefix.RC4.getValue())) {
            globalParamVo.setValue(RC4Util.decrypt(globalParamVo.getValue().substring(4)));
        }
        return globalParamVo;
    }
}
