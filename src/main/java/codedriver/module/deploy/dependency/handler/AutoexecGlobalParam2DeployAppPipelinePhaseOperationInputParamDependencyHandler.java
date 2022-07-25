/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 发布应用流水线阶段操作输入参数映射引用全局参数处理器
 */
@Component
public class AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler extends FixedTableDependencyHandlerBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long appSystemId = config.getLong("appSystemId");
        Long moduleId = config.getLong("moduleId");
        Long envId = config.getLong("envId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId, moduleId, envId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            return null;
        }
        DeployPipelineConfigVo pipelineConfigVo = deployAppConfigVo.getConfig();
        if (pipelineConfigVo == null) {
            return null;
        }
        List<DeployPipelinePhaseVo> combopPhaseList = pipelineConfigVo.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return null;
        }
        Long phaseId = config.getLong("phaseId");
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
            if (!Objects.equals(combopPhaseVo.getId(), phaseId)) {
                continue;
            }
            AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
            if (phaseConfigVo == null) {
                return null;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfigVo.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                return null;
            }
            Long id = Long.valueOf(dependencyVo.getTo());
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                if (!Objects.equals(phaseOperationVo.getId(), id)) {
                    continue;
                }
                AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
                if (operationConfigVo == null) {
                    return null;
                }
                List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
                if (CollectionUtils.isEmpty(paramMappingList)) {
                    return null;
                }
                for (ParamMappingVo paramMappingVo : paramMappingList) {
                    if (paramMappingVo == null) {
                        continue;
                    }
                    if (!Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        continue;
                    }
                    if (Objects.equals(paramMappingVo.getValue(), dependencyVo.getFrom())) {
                        String operationName = phaseOperationVo.getOperationName();
                        String phaseName = combopPhaseVo.getName();
                        String key = config.getString("key");
                        String name = paramMappingVo.getName();
                        if (StringUtils.isBlank(name)) {
                            name = config.getString("name");
                            if (StringUtils.isBlank(name)) {
                                name = key;
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
                        pathList.add(phaseName);
                        pathList.add(operationName);
                        pathList.add("输入参数映射");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("/");
                        stringBuilder.append(TenantContext.get().getTenantUuid());
                        stringBuilder.append("/deploy.html#/application-config-manage?appSystemId=${DATA.appSystemId}");
                        JSONObject dependencyInfoConfig = new JSONObject();
                        dependencyInfoConfig.put("appSystemId", appSystemId);
                        if (moduleId != null && moduleId != 0L) {
                            dependencyInfoConfig.put("moduleId", moduleId);
                            stringBuilder.append("&moduleId=${DATA.moduleId}");
                            if (envId != null && envId != 0L) {
                                dependencyInfoConfig.put("envId", envId);
                                stringBuilder.append("&envId=${DATA.envId}");
                            }
                        }
                        String urlFormat = stringBuilder.toString();
                        String value = id + "_" + key;
                        return new DependencyInfoVo(value, dependencyInfoConfig, name, pathList, urlFormat, this.getGroupName());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.GLOBAL_PARAM;
    }
}
