/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.ADMIN;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineGroupVo;
import neatlogic.framework.deploy.dto.app.DeployPipelinePhaseVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.PipelineService;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ADMIN.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class MoveDeployAppPipelinePhaseBelongGroupApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    PipelineService pipelineService;

    @Override
    public String getName() {
        return "移动阶段到指定组";
    }

    @Input({
            @Param(name = "appSystemAbbrNameList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "应用系统简称列表", help = "isAllAppSystem=0才生效"),
            @Param(name = "phaseList", type = ApiParamType.JSONARRAY,  isRequired = true, desc = "阶段列表", help = "[{\"name\":\"阶段名\",\"groupSort\":\"阶段组序号\"}]"),
            @Param(name = "targetPhaseName", type = ApiParamType.STRING, isRequired = true,  desc = "目标阶段名"),
            @Param(name = "targetPhaseGroupSort", type = ApiParamType.STRING,  isRequired = true, desc = "目标阶段组号"),
            @Param(name = "isSave", type = ApiParamType.ENUM, rule = "0,1",  isRequired = true, desc = "是否保存"),
            @Param(name = "isAllAppSystem", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "是否扫描所有应用系统"),
    })
    @Output({

    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resultList = new JSONArray();
        String targetPhaseName = paramObj.getString("targetPhaseName");
        Integer targetPhaseGroupSort = paramObj.getInteger("targetPhaseGroupSort");
        Integer isSave = paramObj.getInteger("isSave");
        JSONArray phaseArray = paramObj.getJSONArray("phaseList");
        JSONArray appSystemAbbrNameList = paramObj.getJSONArray("appSystemAbbrNameList");
        Integer isAllAppSystem = paramObj.getInteger("isAllAppSystem");
        if (CollectionUtils.isNotEmpty(phaseArray)) {
            IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<Long> appSystemIdList = deployAppConfigMapper.getAppConfigAppSystemIdListByAppSystemIdList(null);
            if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                BasePageVo basePageVo = new BasePageVo();
                List<ResourceVo> appEnvList = resourceCenterDataSource.getAppEnvListForSelect(basePageVo, false);
                Map<Long, String> appEnvId2NameMap = appEnvList.stream().filter(Objects::nonNull).collect(Collectors.toMap(ResourceVo::getId, ResourceVo::getName));
                appEnvId2NameMap.put(-2L, "未配置环境");
                for (Long appSystemId : appSystemIdList) {
                    ResourceVo appSystemVo = resourceCrossoverMapper.getAppSystemById(appSystemId);
                    if (appSystemVo == null) {
                        continue;
                    }
                    if (Objects.equals(isAllAppSystem, 0)) {
                        if (!appSystemAbbrNameList.contains(appSystemVo.getAbbrName())) {
                            continue;
                        }
                    }
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
                            boolean flag = analysis(pipelineConfigVo, phaseArray, targetPhaseName, targetPhaseGroupSort);
                            if (flag) {
                                JSONObject resultObj = new JSONObject(new LinkedHashMap<>());
                                resultObj.put("appSystemId", appSystemId);
                                resultObj.put("appSystemName", appSystemVo.getName());
                                resultObj.put("appSystemAbbrName", appSystemVo.getAbbrName());
                                resultObj.put("appModuleId", deployAppConfigVo.getAppModuleId());
                                AppModuleVo appModuleVo = appModuleMap.get(deployAppConfigVo.getAppModuleId());
                                if (appModuleVo != null) {
                                    resultObj.put("appModuleName", appModuleVo.getName());
                                    resultObj.put("appModuleAbbrName", appModuleVo.getAbbrName());
                                }
                                String envName = appEnvId2NameMap.get(deployAppConfigVo.getEnvId());
                                resultObj.put("envId", deployAppConfigVo.getEnvId());
                                resultObj.put("envName", envName);
                                resultList.add(resultObj);
                                if (Objects.equals(isSave, 1)) {
                                    deployAppConfigVo.setConfig(pipelineConfigVo);
                                    pipelineService.saveDeployAppPipeline(deployAppConfigVo);
                                }
                            }
                        }
                    }
                }
            }
            return resultList;
        }
        return null;
    }

    private boolean analysis(DeployPipelineConfigVo pipelineConfigVo, JSONArray phaseArray, String targetPhaseName, Integer targetPhaseGroupSort) {
        List<DeployPipelinePhaseVo> combopPhaseList = pipelineConfigVo.getCombopPhaseList();
        List<DeployPipelineGroupVo> combopGroupList = pipelineConfigVo.getCombopGroupList();
        Map<Long, DeployPipelineGroupVo> id2DeployPipelineGroupVoMap = combopGroupList.stream().filter(Objects::nonNull).collect(Collectors.toMap(DeployPipelineGroupVo::getId, e -> e));

        DeployPipelinePhaseVo targetPhaseVo = null;
        for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
            if (pipelinePhaseVo != null) {
                if (Objects.equals(pipelinePhaseVo.getName(), targetPhaseName)) {
                    DeployPipelineGroupVo groupVo = id2DeployPipelineGroupVoMap.get(pipelinePhaseVo.getGroupId());
                    if (groupVo != null) {
                        Integer sort = groupVo.getSort();
                        if (sort != null && sort + 1 == targetPhaseGroupSort) {
                            targetPhaseVo = pipelinePhaseVo;
                            break;
                        }
                    }
                }
            }
        }
        if (targetPhaseVo != null) {
            boolean flag = false;
            for (int i = 0; i < phaseArray.size(); i++) {
                JSONObject phaseObj = phaseArray.getJSONObject(i);
                String phaseName = phaseObj.getString("name");
                Integer groupSort = phaseObj.getInteger("groupSort");
                flag = false;
                for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
                    if (pipelinePhaseVo != null) {
                        if (Objects.equals(pipelinePhaseVo.getName(), phaseName) && Objects.equals(pipelinePhaseVo.getGroupSort() + 1, groupSort)) {
                            if (!Objects.equals(pipelinePhaseVo.getGroupId(), targetPhaseVo.getGroupId())) {
                                DeployPipelineGroupVo groupVo = id2DeployPipelineGroupVoMap.get(targetPhaseVo.getGroupId());
                                pipelinePhaseVo.setGroupId(groupVo.getId());
                                pipelinePhaseVo.setGroupUuid(groupVo.getUuid());
                                pipelinePhaseVo.setGroupSort(groupVo.getSort());
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (flag) {
                List<Long> groupIdList = new ArrayList<>();
                for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
                    groupIdList.add(pipelinePhaseVo.getGroupId());
                }
                boolean deleted = false;
                for (int i = combopGroupList.size() - 1; i >= 0; i--) {
                    DeployPipelineGroupVo groupVo = combopGroupList.get(i);
                    if (groupVo != null) {
                        if (!groupIdList.contains(groupVo.getId())) {
                            combopGroupList.remove(i);
                            deleted = true;
                        }
                    } else {
                        combopGroupList.remove(i);
                    }
                }
                if (deleted) {
                    for (int i = 0; i < combopGroupList.size(); i++) {
                        DeployPipelineGroupVo groupVo = combopGroupList.get(i);
                        groupVo.setSort(i);
                    }
                    Map<Long, Integer> id2SortMap = combopGroupList.stream().filter(Objects::nonNull).collect(Collectors.toMap(DeployPipelineGroupVo::getId, DeployPipelineGroupVo::getSort));
                    for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
                        Integer sort = id2SortMap.get(pipelinePhaseVo.getGroupId());
                        pipelinePhaseVo.setGroupSort(sort);
                    }
                }
                List<DeployPipelinePhaseVo> newPhaseList = new ArrayList<>();
                for (DeployPipelineGroupVo groupVo : combopGroupList) {
                    int phaseSort = 0;
                    for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
                        if (Objects.equals(pipelinePhaseVo.getGroupId(), groupVo.getId())) {
                            pipelinePhaseVo.setSort(phaseSort);
                            newPhaseList.add(pipelinePhaseVo);
                            phaseSort++;
                        }
                    }
                }
                pipelineConfigVo.setCombopPhaseList(newPhaseList);
                pipelineConfigVo.setCombopGroupList(combopGroupList);
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
        return "deploy/app/pipeline/phasebelonggroup/move";
    }
}
