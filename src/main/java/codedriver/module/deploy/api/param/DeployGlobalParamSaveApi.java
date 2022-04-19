package codedriver.module.deploy.api.param;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.param.DeployGlobalParamVo;
import codedriver.framework.deploy.exception.param.DeployGlobalParamDisplayNameRepeatException;
import codedriver.framework.deploy.exception.param.DeployGlobalParamIsNotFoundException;
import codedriver.framework.deploy.exception.param.DeployGlobalParamNameRepeatException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployGlobalParamMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/18 6:54 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
public class DeployGlobalParamSaveApi extends PrivateApiComponentBase {

    @Resource
    DeployGlobalParamMapper deployGlobalParamMapper;

    @Override
    public String getName() {
        return "保存发布全局参数";
    }

    @Override
    public String getToken() {
        return "deploy/global/param/save";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键 id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "参数名"),
            @Param(name = "displayName", type = ApiParamType.STRING, isRequired = true, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述")
    })
    @Description(desc = "保存发布全局参数接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployGlobalParamVo globalParamVo = paramObj.toJavaObject(DeployGlobalParamVo.class);
        if (deployGlobalParamMapper.checkGlobalParamNameIsRepeat(globalParamVo) > 0) {
            throw new DeployGlobalParamNameRepeatException(globalParamVo.getName());
        } else if (deployGlobalParamMapper.checkGlobalParamDisplayNameIsRepeat(globalParamVo) > 0) {
            throw new DeployGlobalParamDisplayNameRepeatException(globalParamVo.getName());
        }
        Long paramId = paramObj.getLong("id");
        if (paramId != null && deployGlobalParamMapper.checkGlobalParamIsExistsById(paramId) == 0) {
            throw new DeployGlobalParamIsNotFoundException(paramId);
        }
        deployGlobalParamMapper.insertGlobalParam(globalParamVo);
        return null;
    }

    public IValid name() {
        return value -> {
            DeployGlobalParamVo globalParamVo = JSON.toJavaObject(value, DeployGlobalParamVo.class);
            if (deployGlobalParamMapper.checkGlobalParamNameIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new DeployGlobalParamNameRepeatException(globalParamVo.getName()));
            } else if (deployGlobalParamMapper.checkGlobalParamDisplayNameIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new DeployGlobalParamDisplayNameRepeatException(globalParamVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

    public IValid displayName() {
        return value -> {
            DeployGlobalParamVo globalParamVo = JSON.toJavaObject(value, DeployGlobalParamVo.class);
            if (deployGlobalParamMapper.checkGlobalParamDisplayNameIsRepeat(globalParamVo) > 0) {
                return new FieldValidResultVo(new DeployGlobalParamDisplayNameRepeatException(globalParamVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
