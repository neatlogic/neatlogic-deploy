/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.AutoexecCombopPhaseNameRepeatException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveDeployAppPipelineApi extends PrivateApiComponentBase {

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
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线配置信息")
    })
    @Description(desc = "保存应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        DeployAppConfigVo oldDeployAppConfigVo = deployAppConfigMapper.getAppConfigVo(deployAppConfigVo);
        if (oldDeployAppConfigVo != null) {
            DeployPipelineConfigVo oldConfigVo = oldDeployAppConfigVo.getConfig();
            List<AutoexecParamVo> autoexecParamList = oldConfigVo.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(autoexecParamList)) {
                DeployPipelineConfigVo newConfigVo = deployAppConfigVo.getConfig();
                newConfigVo.setRuntimeParamList(autoexecParamList);
                deployAppConfigVo.setConfig(newConfigVo);
            }
            if (Objects.equals(oldDeployAppConfigVo.getConfigStr(), deployAppConfigVo.getConfigStr())) {
                //如果没有改动，不用更新数据库数据
                return null;
            }
        }
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();
        if (envId != null && envId != 0) {
            //环境层，需要对重载过的阶段的操作重新生成id
            regenerateOperationId(deployAppConfigVo);
        } else if (moduleId != null && moduleId != 0) {
            //模块层，需要对重载过的阶段的操作重新生成id
            regenerateOperationId(deployAppConfigVo);
        } else {
            //应用层，在首次保存时需要重新生成阶段id和操作id
            if (oldDeployAppConfigVo == null) {
                regeneratePhaseIdAndOperationId(deployAppConfigVo);
            }
        }
        deployAppConfigVo.setConfigStr(null);
        IAutoexecCombopCrossoverService autoexecCombopCrossoverService = CrossoverServiceFactory.getApi(IAutoexecCombopCrossoverService.class);
        autoexecCombopCrossoverService.verifyAutoexecCombopConfig(deployAppConfigVo.getConfig().getAutoexecCombopConfigVo(), false);
        if (oldDeployAppConfigVo != null) {
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
     * 重新生成阶段id和操作id
     * @param deployAppConfigVo
     */
    private void regeneratePhaseIdAndOperationId(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        List<String> nameList = new ArrayList<>();
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            String name = combopPhaseVo.getName();
            if (nameList.contains(name)) {
                throw new AutoexecCombopPhaseNameRepeatException(name);
            }
            nameList.add(name);
            combopPhaseVo.setId(null);
            regenerateOperationId(combopPhaseVo);
        }
    }
    /**
     * 重新生成操作id
     * @param deployAppConfigVo
     */
    private void regenerateOperationId(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if(!Objects.equals(combopPhaseVo.getOverride(), 1)) {
                continue;
            }
            regenerateOperationId(combopPhaseVo);
        }
    }
    /**
     * 重新生成操作id
     * @param combopPhaseVo
     */
    private void regenerateOperationId(AutoexecCombopPhaseVo combopPhaseVo) {
        AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
        if (phaseConfig == null) {
            return;
        }
        List<AutoexecCombopPhaseOperationVo> operationList = phaseConfig.getPhaseOperationList();
        if (CollectionUtils.isEmpty(operationList)) {
            return;
        }
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : operationList) {
            if(phaseOperationVo == null) {
                continue;
            }
            phaseOperationVo.setId(null);
            AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
            List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                    if (operationVo == null) {
                        continue;
                    }
                    operationVo.setId(null);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                    if (operationVo == null) {
                        continue;
                    }
                    operationVo.setId(null);
                }
            }
        }
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
        Long moduleId = deployAppConfigVo.getAppModuleId();
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
                if (phaseOperationVo == null) {
                    continue;
                }
                saveDependency(combopPhaseVo, phaseOperationVo, appSystemId, moduleId, envId);
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
                    }
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
    private void saveDependency(AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo, Long appSystemId, Long moduleId, Long envId) {
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
            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
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
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
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
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
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
        Long moduleId = deployAppConfigVo.getAppModuleId();
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
                if (phaseOperationVo == null) {
                    continue;
                }
                DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, phaseOperationVo.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, phaseOperationVo.getId());
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, operationVo.getId());
                        DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, operationVo.getId());
                    }
                }
            }
        }
    }
}
