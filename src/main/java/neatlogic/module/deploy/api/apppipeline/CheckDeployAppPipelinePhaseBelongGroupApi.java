/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineGroupVo;
import neatlogic.framework.deploy.dto.app.DeployPipelinePhaseVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class CheckDeployAppPipelinePhaseBelongGroupApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "检查两个阶段是否在同一个组";
    }

    @Input({
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
//            @Param(name = "phaseGroupSort", type = ApiParamType.STRING, desc = "阶段组号"),
            @Param(name = "targetPhaseName", type = ApiParamType.STRING, desc = "目标阶段名"),
//            @Param(name = "targetPhaseGroupSort", type = ApiParamType.STRING, desc = "阶段组号"),
    })
    @Output({

    })
    @Description(desc = "检查两个阶段是否在同一个组")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resultList = new JSONArray();
        String phaseName = paramObj.getString("phaseName");
        String targetPhaseName = paramObj.getString("targetPhaseName");
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<Long> appSystemIdList = deployAppConfigMapper.getAppConfigAppSystemIdListByAppSystemIdList(null);
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            BasePageVo basePageVo = new BasePageVo();
            List<ResourceVo> appEnvList = resourceCenterDataSource.getAppEnvListForSelect(basePageVo, false);
            Map<Long, String> appEnvId2NameMap = appEnvList.stream().filter(Objects::nonNull).collect(Collectors.toMap(ResourceVo::getId, ResourceVo::getName));
            appEnvId2NameMap.put(-2L, "未配置环境");
            for (Long appSystemId : appSystemIdList) {
                List<AppModuleVo> appModuleList = resourceCenterDataSource.getAppModuleListForTree(appSystemId);
                Map<Long, AppModuleVo> appModuleMap = appModuleList.stream().filter(Objects::nonNull).collect(Collectors.toMap(AppModuleVo::getId, e -> e));
                Set<Long> appModuleIdSet = new HashSet<>();
                Set<Long> envIdSet = new HashSet<>();
                List<DeployAppConfigVo> appConfigList = deployAppConfigMapper.getAppConfigListByAppSystemIdAndAppModuleIdListAndEnvIdList(appSystemId, null, null);
                for (DeployAppConfigVo deployAppConfigVo : appConfigList) {
                    appModuleIdSet.add(deployAppConfigVo.getAppModuleId());
                    envIdSet.add(deployAppConfigVo.getEnvId());
                }
                List<DeployAppConfigVo> deployAppConfigList = DeployPipelineConfigManager.init(appSystemId)
                        .withAppModuleIdList(new ArrayList<>(appModuleIdSet))
                        .withEnvIdList(new ArrayList<>(envIdSet))
                        .withIsHasBuildOrDeployTypeTool(false)
                        .withIsUpdateConfig(true)
                        .withIsUpdateProfile(true)
                        .getDeployAppConfigList();
                for (DeployAppConfigVo deployAppConfigVo : appConfigList) {
                    DeployPipelineConfigVo pipelineConfigVo = getDeployPipelineConfigVo(deployAppConfigList, appSystemId, deployAppConfigVo.getAppModuleId(), deployAppConfigVo.getEnvId());
                    if (pipelineConfigVo != null) {
                        JSONObject resultObj = new JSONObject(new LinkedHashMap<>());
                        resultObj.put("appSystemId", appSystemId);
                        ResourceVo appSystemVo = resourceCrossoverMapper.getAppSystemById(appSystemId);
                        if (appSystemVo != null) {
                            resultObj.put("appSystemName", appSystemVo.getName());
                            resultObj.put("appSystemAbbrName", appSystemVo.getAbbrName());
                        }
                        resultObj.put("appModuleId", deployAppConfigVo.getAppModuleId());
                        AppModuleVo appModuleVo = appModuleMap.get(deployAppConfigVo.getAppModuleId());
                        if (appModuleVo != null) {
                            resultObj.put("appModuleName", appModuleVo.getName());
                            resultObj.put("appModuleAbbrName", appModuleVo.getAbbrName());
                        }
                        String envName = appEnvId2NameMap.get(deployAppConfigVo.getEnvId());
                        resultObj.put("envId", deployAppConfigVo.getEnvId());
                        resultObj.put("envName", envName);
                        boolean flag = analysis(pipelineConfigVo, phaseName, targetPhaseName, resultObj);
                        if (flag) {
                            resultList.add(resultObj);
                        }
                    }
                }
            }
        }
        return resultList;
    }

    private boolean analysis(DeployPipelineConfigVo pipelineConfigVo, String phaseName, String targetPhaseName, JSONObject resultObj) {
        List<DeployPipelinePhaseVo> combopPhaseList = pipelineConfigVo.getCombopPhaseList();
        DeployPipelinePhaseVo phaseVo = null;
        DeployPipelinePhaseVo targetPhaseVo = null;
        for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
            if (pipelinePhaseVo != null) {
                if (Objects.equals(pipelinePhaseVo.getName(), phaseName)) {
                    phaseVo = pipelinePhaseVo;
                } else if (Objects.equals(pipelinePhaseVo.getName(), targetPhaseName)) {
                    targetPhaseVo = pipelinePhaseVo;
                }
            }
        }
        if (phaseVo != null && targetPhaseVo != null) {
            if (!Objects.equals(phaseVo.getGroupId(), targetPhaseVo.getGroupId())) {
                List<DeployPipelineGroupVo> combopGroupList = pipelineConfigVo.getCombopGroupList();
                Map<Long, Integer> id2SortMap = combopGroupList.stream().filter(Objects::nonNull).collect(Collectors.toMap(DeployPipelineGroupVo::getId, DeployPipelineGroupVo::getSort));
                {
                    resultObj.put("phaseName", phaseName);
                    Integer sort = id2SortMap.get(phaseVo.getGroupId());
                    if (sort != null) {
                        resultObj.put("phaseGroupSort", (sort + 1));
                    }
                }
                {
                    resultObj.put("targetPhaseName", targetPhaseName);
                    Integer sort = id2SortMap.get(targetPhaseVo.getGroupId());
                    if (sort != null) {
                        resultObj.put("targetPhaseGroupSort", (sort + 1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    private DeployPipelineConfigVo getDeployPipelineConfigVo(List<DeployAppConfigVo> deployAppConfigList, Long appSystemId, Long appModuleId, Long envId) {
        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
                    && Objects.equals(deployAppConfigVo.getAppModuleId(), appModuleId)
                    && Objects.equals(deployAppConfigVo.getEnvId(), envId)) {
                return deployAppConfigVo.getConfig();
            }
        }
        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
                    && Objects.equals(deployAppConfigVo.getAppModuleId(), appModuleId)
                    && Objects.equals(deployAppConfigVo.getEnvId(), 0L)) {
                return deployAppConfigVo.getConfig();
            }
        }
        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
                    && Objects.equals(deployAppConfigVo.getAppModuleId(), 0L)
                    && Objects.equals(deployAppConfigVo.getEnvId(), 0L)) {
                return deployAppConfigVo.getConfig();
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/phasebelonggroup/check";
    }
}
