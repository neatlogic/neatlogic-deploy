/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neatlogic.module.deploy.dependency.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
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
