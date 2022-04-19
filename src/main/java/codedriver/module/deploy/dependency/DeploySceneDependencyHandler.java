package codedriver.module.deploy.dependency;

import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.deploy.constvalue.DeployFromType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/19 3:41 下午
 */
@Service
public class DeploySceneDependencyHandler extends CustomTableDependencyHandlerBase {

    @Override
    protected String getTableName() {
        return "deploy_scene_cientity";
    }

    @Override
    protected String getFromField() {
        return "scene_id";
    }

    @Override
    protected String getToField() {
        return "ci_entity_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        return null;
    }

    @Override
    public IFromType getFromType() {
        return DeployFromType.DEPLOY_SCENE_CIENTITY;
    }
}
