/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppPipelineProfileListApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Override
    public String getName() {
        return "获取应用流水线预置参数集列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/List";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值")
    })
    @Output({
            @Param(name = "Return", explode = DeployProfileVo[].class, desc = "应用流水线预置参数集列表")
    })
    @Description(desc = "获取应用流水线预置参数集列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigOverrideVo deployAppConfigOverrideVo = paramObj.toJavaObject(DeployAppConfigOverrideVo.class);
        Long moduleId = deployAppConfigOverrideVo.getModuleId();
        Long envId = deployAppConfigOverrideVo.getEnvId();
        // 如果是访问环境层配置信息，moduleId不能为空
        if (moduleId == 0L && envId != 0L) {
            throw new ParamNotExistsException("moduleId");
        }
        DeployPipelineConfigVo config = deployAppPipelineService.getDeployPipelineConfigVo(deployAppConfigOverrideVo);
        Set<Long> profileIdSet = new HashSet<>();
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> profileIdList = defaultValue.toJavaList(Long.class);
            profileIdSet.addAll(profileIdList);
        } else {
            List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isEmpty(combopPhaseList)) {
                return new ArrayList<>();
            }
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo == null) {
                    continue;
                }
                List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
                if (CollectionUtils.isEmpty(combopPhaseOperationList)) {
                    continue;
                }
                for (AutoexecCombopPhaseOperationVo combopPhaseOperationVo : combopPhaseOperationList) {
                    AutoexecCombopPhaseOperationConfigVo operationConfigVo = combopPhaseOperationVo.getConfig();
                    if (operationConfigVo == null) {
                        continue;
                    }
                    Long profileId = operationConfigVo.getProfileId();
                    if (profileId != null) {
                        profileIdSet.add(profileId);
                    }
                }
            }
        }

        if (CollectionUtils.isEmpty(profileIdSet)) {
            return new ArrayList<>();
        }
        IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
        List<AutoexecProfileVo> profileList = autoexecProfileCrossoverService.getProfileVoListByIdList(new ArrayList<>(profileIdSet));
        if (CollectionUtils.isEmpty(profileList)) {
            return new ArrayList<>();
        }
        List<DeployProfileVo> deployProfileList = getDeployProfileList(profileList);
        List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
        overrideProfile(deployProfileList, overrideProfileList);
        return deployProfileList;
    }

    private List<DeployProfileVo> getDeployProfileList(List<AutoexecProfileVo> profileList) {
        List<DeployProfileVo> deployProfileList = new ArrayList<>();
        for (AutoexecProfileVo autoexecProfileVo : profileList) {
            DeployProfileVo deployProfileVo = new DeployProfileVo();
            deployProfileVo.setProfileId(autoexecProfileVo.getId());
            deployProfileVo.setProfileName(autoexecProfileVo.getName());
            List<AutoexecProfileParamVo> profileParamList = autoexecProfileVo.getProfileParamVoList();
            if (CollectionUtils.isNotEmpty(profileParamList)) {
                List<DeployProfileParamVo> deployProfileParamList = new ArrayList<>();
                for (AutoexecProfileParamVo autoexecProfileParamVo : profileParamList) {
                    DeployProfileParamVo deployProfileParamVo = new DeployProfileParamVo();
                    deployProfileParamVo.setKey(autoexecProfileParamVo.getKey());
                    deployProfileParamVo.setValue(autoexecProfileParamVo.getDefaultValue());
                    deployProfileParamVo.setDescription(autoexecProfileParamVo.getDescription());
                    deployProfileParamVo.setType(autoexecProfileParamVo.getType());
                    deployProfileParamVo.setInherit(1);
                    deployProfileParamVo.setSource("预置参数集");
                    deployProfileParamList.add(deployProfileParamVo);
                }
                deployProfileVo.setParamList(deployProfileParamList);
            }
            deployProfileList.add(deployProfileVo);
        }
        return deployProfileList;
    }

    private void overrideProfile(List<DeployProfileVo> deployProfileList, List<DeployProfileVo> overrideDeployProfileList) {
        if ( CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo deployProfile : deployProfileList) {
            for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
                if (Objects.equals(deployProfile.getProfileId(), overrideDeployProfile.getProfileId())) {
                    List<DeployProfileParamVo> deployProfileParamList = deployProfile.getParamList();
                    List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
                    overrideProfileParam(deployProfileParamList, overrideDeployProfileParamList);
                    break;
                }
            }
        }
    }

    private void overrideProfileParam(List<DeployProfileParamVo> deployProfileParamList, List<DeployProfileParamVo> overrideDeployProfileParamList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
            return;
        }
        for (DeployProfileParamVo deployProfileParam : deployProfileParamList) {
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (overrideDeployProfileParam.getInherit() == 1) {
                    continue;
                }
                if (Objects.equals(deployProfileParam.getKey(), overrideDeployProfileParam.getKey())) {
                    deployProfileParam.setInherit(overrideDeployProfileParam.getInherit());
                    deployProfileParam.setSource(overrideDeployProfileParam.getSource());
                    deployProfileParam.setValue(overrideDeployProfileParam.getValue());
                    break;
                }
            }
        }
    }
}
