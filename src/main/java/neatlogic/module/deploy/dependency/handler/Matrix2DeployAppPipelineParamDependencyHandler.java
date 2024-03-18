/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.FixedTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 发布应用流水线作业参数引用矩阵关系处理器
 **/
@Service
public class Matrix2DeployAppPipelineParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

//    @Override
//    protected String getTableName() {
//        return "autoexec_combop_param_matrix";
//    }
//
//    @Override
//    protected String getFromField() {
//        return "matrix_uuid";
//    }
//
//    @Override
//    protected String getToField() {
//        return "combop_id";
//    }
//
//    @Override
//    protected List<String> getToFieldList() {
//        List<String> result = new ArrayList<>();
//        result.add("combop_id");
//        result.add("key");
//        return result;
//    }

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long appSystemId = config.getLong("appSystemId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            return null;
        }
        DeployPipelineConfigVo pipelineConfigVo = deployAppConfigVo.getConfig();
        if (pipelineConfigVo == null) {
            return null;
        }
        List<AutoexecParamVo> runtimeParamList = pipelineConfigVo.getRuntimeParamList();
        if (CollectionUtils.isEmpty(runtimeParamList)) {
            return null;
        }
        Long paramId = Long.valueOf(dependencyVo.getTo());
        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
            if (!Objects.equals(autoexecParamVo.getId(), paramId)) {
                continue;
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
            pathList.add("作业参数");
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("appSystemId", appSystemId);
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/deploy.html#/application-config-manage?appSystemId=${DATA.appSystemId}";
            return new DependencyInfoVo(paramId, dependencyInfoConfig, autoexecParamVo.getName(), pathList, urlFormat, this.getGroupName());
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.MATRIX;
    }
}
