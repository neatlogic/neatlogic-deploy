package codedriver.module.deploy.api.scene;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.scene.DeploySceneVo;
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
 * @date 2022/4/15 11:45 上午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
public class DeploySceneSaveApi extends PrivateApiComponentBase {

    @Resource
    DeploySceneMapper deploySceneMapper;

    @Override
    public String getName() {
        return "保存发布场景";
    }

    @Override
    public String getToken() {
        return "deploy/scene/save";
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
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeploySceneVo paramSceneVo = paramObj.toJavaObject(DeploySceneVo.class);
        Long paramId = paramObj.getLong("id");
        if (paramId != null && deploySceneMapper.checkSceneIsExistsById(paramId) == 0) {
            throw new DeploySceneIsNotFoundException(paramId);
        }
        deploySceneMapper.insertScene(paramSceneVo);
        return null;
    }
}
