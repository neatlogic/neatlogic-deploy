/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package neatlogic.module.deploy.dependency.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
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
