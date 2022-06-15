/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.ParamMappingVo;
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
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            Long operationId = Long.getLong(dependencyVo.getTo());
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                if (!Objects.equals(phaseOperationVo.getOperationId(), operationId)) {
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
                        String operationName = phaseOperationVo.getName();
                        String phaseName = combopPhaseVo.getName();
                        String key = config.getString("key");
                        String name = config.getString("name");
                        JSONObject dependencyInfoConfig = new JSONObject();
                        dependencyInfoConfig.put("appSystemId", appSystemId);
                        dependencyInfoConfig.put("moduleId", moduleId);
                        dependencyInfoConfig.put("envId", envId);
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
                        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/deploy.html#/action-detail?appSystemId=${DATA.appSystemId}&moduleId=${DATA.moduleId}&envId=${DATA.envId}";
                        String value = operationId + "_" + key;
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
