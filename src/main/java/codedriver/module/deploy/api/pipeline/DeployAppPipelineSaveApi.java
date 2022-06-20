/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import codedriver.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class DeployAppPipelineSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线配置信息")
    })
    @Description(desc = "保存应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        DeployAppConfigVo oldDeployAppConfigVo = deployAppConfigMapper.getAppConfigVo(deployAppConfigVo);
        if (oldDeployAppConfigVo != null) {
            if (Objects.equals(oldDeployAppConfigVo.getConfigStr(), deployAppConfigVo.getConfigStr())) {
                return null;
            }
            deleteDependency(oldDeployAppConfigVo);
            deployAppConfigVo.setLcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
            saveDependency(deployAppConfigVo);
            deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigVo);
        } else {
            deployAppConfigVo.setFcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
            saveDependency(deployAppConfigVo);
        }
        return null;
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     * @param deployAppConfigVo
     */
    private void saveDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long moduleId = deployAppConfigVo.getModuleId();
        Long envId = deployAppConfigVo.getEnvId();
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo != null) {
                    saveDependency(combopPhaseVo, phaseOperationVo, appSystemId, moduleId, envId);
                }
            }
        }
    }
    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     * @param combopPhaseVo
     * @param phaseOperationVo
     * @param appSystemId
     * @param moduleId
     * @param envId
     */
    private void saveDependency(DeployPipelinePhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo, Long appSystemId, Long moduleId, Long envId) {
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
        if (operationConfigVo == null) {
            return;
        }
        Long profileId = operationConfigVo.getProfileId();
        if (profileId != null) {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("appSystemId", appSystemId);
            dependencyConfig.put("moduleId", moduleId);
            dependencyConfig.put("envId", envId);
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getOperationId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("key", paramMappingVo.getKey());
                    dependencyConfig.put("name", paramMappingVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getOperationId(), dependencyConfig);
                }
            }
        }
    }

    /**
     * 删除阶段中操作工具对预置参数集和全局参数的引用关系
     * @param deployAppConfigVo
     */
    private void deleteDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        Long moduleId = deployAppConfigVo.getModuleId();
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo != null) {
                    DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, phaseOperationVo.getOperationId());
                    DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, phaseOperationVo.getOperationId());
                    DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, phaseOperationVo.getOperationId());
                }
            }
        }
    }
}
