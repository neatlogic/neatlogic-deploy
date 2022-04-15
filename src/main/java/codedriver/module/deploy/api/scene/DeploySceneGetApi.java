package codedriver.module.deploy.api.scene;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.exception.scene.DeploySceneIsNotFoundException;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeploySceneMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/4/15 5:13 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
public class DeploySceneGetApi extends PrivateApiComponentBase {


    @Resource
    DeploySceneMapper deploySceneMapper;

    @Override
    public String getName() {
        return "保存发布场景";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/scene/save";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramId = paramObj.getLong("id");
        if (deploySceneMapper.checkSceneIsExistsById(paramId) == 0) {
            throw new DeploySceneIsNotFoundException(paramId);
        }
        return deploySceneMapper.getSceneById(paramId);
    }
}
