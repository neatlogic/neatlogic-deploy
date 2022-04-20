package codedriver.module.deploy.api.scenaio;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.scenario.DeployScenarioVo;
import codedriver.framework.deploy.exception.scenario.DeployScenarioIsNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployScenarioMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/15 5:13 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployScenarioGetApi extends PrivateApiComponentBase {

    @Resource
    DeployScenarioMapper deployScenarioMapper;

    @Override
    public String getName() {
        return "获取发布场景";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/scenario/get";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "场景id")
    })
    @Description(desc = "获取发布场景接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        DeployScenarioVo scenarioVo = deployScenarioMapper.getScenarioById(paramId);
        if (scenarioVo == null) {
            throw new DeployScenarioIsNotFoundException(paramId);
        }
        return scenarioVo;
    }
}
