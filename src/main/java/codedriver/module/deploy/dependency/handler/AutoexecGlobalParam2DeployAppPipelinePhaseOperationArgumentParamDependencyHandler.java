/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dependency.handler;

import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.GLOBAL_PARAM;
    }

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyObj) {
        return null;
    }
}
