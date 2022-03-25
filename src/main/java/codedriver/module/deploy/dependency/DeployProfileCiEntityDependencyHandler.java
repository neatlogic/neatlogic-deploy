package codedriver.module.deploy.dependency;

import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/25 2:07 下午
 */
public class DeployProfileCiEntityDependencyHandler extends CustomTableDependencyHandlerBase {

    @Override
    protected String getTableName() {
        return "deploy_profile_cientity";
    }

    @Override
    protected String getFromField() {
        return "profile_id";
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
        return null;
    }
}
