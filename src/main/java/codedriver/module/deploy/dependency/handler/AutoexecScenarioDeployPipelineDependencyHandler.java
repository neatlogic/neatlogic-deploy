/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/10/14 18:31
 */

@Component
public class AutoexecScenarioDeployPipelineDependencyHandler extends FixedTableDependencyHandlerBase {
    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        /*暂时前端没有需要依赖跳转，此方法暂时不会被调用*/

        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long configId = Long.valueOf(dependencyVo.getTo());

        Long appSystemId = config.getLong("appSystemId");
        Long moduleId = config.getLong("moduleId") != 0L ? config.getLong("moduleId") : null;
        Long envId = config.getLong("envId") != 0L ? config.getLong("envId") : null;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/");
        stringBuilder.append(TenantContext.get().getTenantUuid());
        stringBuilder.append("/deploy.html#/application-config-pipeline-detail?appSystemId=${DATA.appSystemId}");
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("appSystemId", appSystemId);
        if (moduleId != null && moduleId != 0L) {
            dependencyInfoConfig.put("moduleId", moduleId);
            stringBuilder.append("&appModuleId=${DATA.moduleId}");
            if (envId != null && envId != 0L) {
                dependencyInfoConfig.put("envId", envId);
                stringBuilder.append("&envId=${DATA.envId}");
            }
        }
        List<String> pathList = new ArrayList<>();
        pathList.add("应用配置");
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (appSystemId != null && appSystemId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }
        if (moduleId != null && moduleId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(moduleId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }
        if (envId != null && envId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }

        String urlFormat = stringBuilder.toString();
        return new DependencyInfoVo(configId, dependencyInfoConfig, "发布流水线", pathList, urlFormat, this.getGroupName());

    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCENARIO;
    }
}