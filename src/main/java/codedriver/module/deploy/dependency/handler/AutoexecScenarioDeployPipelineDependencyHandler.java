/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.dependency.handler;

import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import org.springframework.stereotype.Component;

/**
 * @author longrf
 * @date 2022/10/14 18:31
 */

@Component
public class AutoexecScenarioDeployPipelineDependencyHandler extends FixedTableDependencyHandlerBase {
    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCENARIO;
    }
}
