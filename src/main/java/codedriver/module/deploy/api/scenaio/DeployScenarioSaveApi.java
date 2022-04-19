package codedriver.module.deploy.api.scenaio;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.scenario.DeployScenarioVo;
import codedriver.framework.deploy.exception.scenario.DeployScenarioIsNotFoundException;
import codedriver.framework.deploy.exception.scenario.DeployScenarioRepeatException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployScenarioMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/15 11:45 上午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class DeployScenarioSaveApi extends PrivateApiComponentBase {

    @Resource
    DeployScenarioMapper deployScenarioMapper;

    @Override
    public String getName() {
        return "保存发布场景";
    }

    @Override
    public String getToken() {
        return "deploy/scenario/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键 id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述")
    })
    @Description(desc = "保存发布场景接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployScenarioVo paramScenarioVo = paramObj.toJavaObject(DeployScenarioVo.class);
        if (deployScenarioMapper.checkScenarioNameIsRepeat(paramScenarioVo) > 0) {
            throw new DeployScenarioRepeatException(paramScenarioVo.getName());
        }
        Long paramId = paramObj.getLong("id");
        if (paramId != null && deployScenarioMapper.checkScenarioIsExistsById(paramId) == 0) {
            throw new DeployScenarioIsNotFoundException(paramId);
        }
        deployScenarioMapper.insertScenario(paramScenarioVo);
        return null;
    }

    public IValid name() {
        return value -> {
            DeployScenarioVo paramScenarioVo = JSON.toJavaObject(value, DeployScenarioVo.class);
            if (deployScenarioMapper.checkScenarioNameIsRepeat(paramScenarioVo) > 0) {
                return new FieldValidResultVo(new DeployScenarioRepeatException(paramScenarioVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
