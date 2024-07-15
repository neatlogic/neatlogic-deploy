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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.ModuleVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineProfileParamOverrideApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/paramoverride/List";
    }

    @Override
    public String getName() {
        return "获取当前应用下游对某个预置参数修改的列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "profileId", type = ApiParamType.LONG, isRequired = true, desc = "预置参数集ID"),
            @Param(name = "key", type = ApiParamType.STRING, isRequired = true, desc = "参数key")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "数据列表")
    })
    @Description(desc = "获取当前应用下游对某个预置参数修改的列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<ValueTextVo> tbodyList = new ArrayList<>();
        resultObj.put("tbodyList", tbodyList);
        String key = paramObj.getString("key");
        Long profileId = paramObj.getLong("profileId");
        AutoexecParamVo originalProfileParamVo = null;
        IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
        List<AutoexecProfileParamVo> profileParamList = autoexecProfileCrossoverService.getProfileParamListById(profileId);
        for (AutoexecProfileParamVo profileParamVo : profileParamList) {
            if (Objects.equals(profileParamVo.getKey(), key)) {
                originalProfileParamVo = profileParamVo;
                ValueTextVo valueTextVo = new ValueTextVo(profileParamVo, "预置参数");
                tbodyList.add(valueTextVo);
                break;
            }
        }

        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        //查询应用层配置信息
        Long appSystemId = paramObj.getLong("appSystemId");
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
        AutoexecParamVo paramVo = null;
        Map<String, AutoexecParamVo> paramMap = new HashMap<>();
        String appSystemName = StringUtils.isBlank(appSystem.getName()) ? appSystem.getAbbrName() : appSystem.getName();
        if (StringUtils.isNotBlank(configStr)) {
            DeployPipelineConfigVo config = JSON.parseObject(configStr, DeployPipelineConfigVo.class);
            paramVo = findDeployProfileParamByProfileIdAndKey(config, profileId, key);
            if (paramVo != null && originalProfileParamVo != null) {
                DeployProfileParamVo deployProfileParamVo = new DeployProfileParamVo(originalProfileParamVo);
                deployProfileParamVo.setDefaultValue(paramVo.getDefaultValue());
                paramVo = deployProfileParamVo;
            }

        }
        paramMap.put(appSystemId.toString(), paramVo);
        ValueTextVo valueTextVo = new ValueTextVo(paramVo,appSystemName);
        tbodyList.add(valueTextVo);

        //查询应用层下游的配置信息
        List<DeployAppConfigVo> deployAppConfigList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isNotEmpty(deployAppConfigList)) {
            for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
                DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
                if (config == null) {
                    continue;
                }
                paramVo = findDeployProfileParamByProfileIdAndKey(config, profileId, key);
                if (paramVo != null && originalProfileParamVo != null) {
                    DeployProfileParamVo deployProfileParamVo = new DeployProfileParamVo(originalProfileParamVo);
                    deployProfileParamVo.setDefaultValue(paramVo.getDefaultValue());
                    paramVo = deployProfileParamVo;
                    List<String> idList = new ArrayList<>();
                    idList.add(appSystemId.toString());
                    Long moduleId = deployAppConfigVo.getAppModuleId();
                    if (moduleId != null && moduleId != 0L) {
                        idList.add(moduleId.toString());
                    }
                    Long envId = deployAppConfigVo.getEnvId();
                    if (envId != null && envId != 0L) {
                        idList.add(envId.toString());
                    }
                    paramMap.put(String.join("-", idList), paramVo);
                }
            }
        }
        // 查询出当前应用下游节点
        Map<Long, String> nameMap = new HashMap<>();
        List<DeployAppConfigVo> allDeployAppConfigList = new ArrayList<>();
        List<Long> appSystemIdList = new ArrayList<>();
        appSystemIdList.add(appSystemId);
        ResourceSearchVo searchVo = new ResourceSearchVo();
        searchVo.setAppSystemIdList(appSystemIdList);
        List<ModuleVo> appModuleList = resourceCrossoverMapper.getAppModuleListByAppSystemIdList(searchVo);
        if (CollectionUtils.isNotEmpty(appModuleList)) {
            for (ModuleVo appModule : appModuleList) {
                allDeployAppConfigList.add(new DeployAppConfigVo(appSystemId, appModule.getAppModuleId()));
                nameMap.put(appModule.getAppModuleId(), StringUtils.isNotBlank(appModule.getAppModuleName()) ? appModule.getAppModuleName() : appModule.getAppModuleAbbrName());
                List<DeployAppEnvironmentVo> envList = deployAppConfigMapper.getCmdbEnvListByAppSystemIdAndModuleId(appSystemId, appModule.getAppModuleId());
                List<DeployAppEnvironmentVo> deployEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, appModule.getAppModuleId());
                envList.addAll(deployEnvList);
                if (CollectionUtils.isNotEmpty(envList)) {
                    for (DeployAppEnvironmentVo appEnvironmentVo : envList) {
                        nameMap.put(appEnvironmentVo.getId(), appEnvironmentVo.getName());
                        allDeployAppConfigList.add(new DeployAppConfigVo(appSystemId, appModule.getAppModuleId(), appEnvironmentVo.getId()));
                    }
                }
            }
        }

        for (DeployAppConfigVo deployAppConfigVo : allDeployAppConfigList) {
            List<String> idList = new ArrayList<>();
            idList.add(appSystemId.toString());
            List<String> nameList = new ArrayList<>();
            nameList.add(appSystemName);
            Long moduleId = deployAppConfigVo.getAppModuleId();
            if (moduleId != null && moduleId != 0L) {
                idList.add(moduleId.toString());
                String name = nameMap.get(moduleId);
                if (name != null) {
                    nameList.add(name);
                } else {
                    nameList.add("undefined");
                }
            }
            Long envId = deployAppConfigVo.getEnvId();
            if (envId != null && envId != 0L) {
                idList.add(envId.toString());
                String name = nameMap.get(envId);
                if (name != null) {
                    nameList.add(name);
                } else {
                    nameList.add("undefined");
                }
            }
            while (idList.size() > 0) {
                String idStr = String.join("-", idList);
                paramVo = paramMap.get(idStr);
                if (paramVo != null) {
                    break;
                }
                idList.remove(idList.size() - 1);
            }
            valueTextVo = new ValueTextVo(paramVo, String.join(" / ", nameList));
            tbodyList.add(valueTextVo);
        }
        return resultObj;
    }

    /**
     * 从流水线配置信息中找出某预置参数数据
     *
     * @param config    流水线配置信息
     * @param profileId 预置参数集ID
     * @param key       参数key
     * @return
     */
    public DeployProfileParamVo findDeployProfileParamByProfileIdAndKey(DeployPipelineConfigVo config, Long profileId, String key) {
        List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
        if (CollectionUtils.isEmpty(overrideProfileList)) {
            return null;
        }
        for (DeployProfileVo deployProfileVo : overrideProfileList) {
            if (Objects.equals(deployProfileVo.getProfileId(), profileId)) {
                List<DeployProfileParamVo> paramList = deployProfileVo.getParamList();
                if (CollectionUtils.isNotEmpty(paramList)) {
                    for (DeployProfileParamVo paramVo : paramList) {
                        if (Objects.equals(paramVo.getInherit(), 1)) {
                            continue;
                        }
                        if (Objects.equals(paramVo.getKey(), key)) {
                            return paramVo;
                        }
                    }
                }
            }
        }
        return null;
    }
}
