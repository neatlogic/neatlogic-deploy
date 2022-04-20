package codedriver.module.deploy.api.scenaio;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployFromType;
import codedriver.framework.deploy.dto.scenario.DeployScenarioVo;
import codedriver.framework.deploy.exception.scenario.DeployScenarioHasBeenReferredException;
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
 * @date 2022/4/19 2:16 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeployScenarioDeleteApi extends PrivateApiComponentBase {

    @Resource
    DeployScenarioMapper deployScenarioMapper;

    @Override
    public String getName() {
        return "删除发布场景";
    }

    @Override
    public String getToken() {
        return "deploy/scenario/delete";
    }
    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "场景id")
    })
    @Description(desc = "删除发布场景接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        DeployScenarioVo paramScenarioVo = deployScenarioMapper.getScenarioById(paramId);
        if (paramScenarioVo == null) {
            throw new DeployScenarioIsNotFoundException(paramId);
        }
        //TODO 插入应用与场景关系的接口未完善，后续需要注意关系表的表结构
        if (DependencyManager.getDependencyCount(DeployFromType.DEPLOY_SCENARIO_CIENTITY, paramId) > 0) {
            throw new DeployScenarioHasBeenReferredException(paramScenarioVo.getName());
        }
        deployScenarioMapper.deleteScenarioById(paramId);
        return null;
    }
}
