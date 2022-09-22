/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.deploy.exception.DeployAppConfigScenarioNotFoundException;
import codedriver.framework.deploy.exception.DeployAppConfigScenarioPhaseNameListNotFoundException;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/7/11 6:08 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployJobModuleApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "根据系统、环境、场景查询一键发布页面的模块信息列表";
    }

    @Override
    public String getToken() {
        return "deploy/job/module/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "scenarioId", type = ApiParamType.LONG, isRequired = true, desc = "场景id")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "根据系统、环境、场景查询一键发布页面的模块信息列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        List<DeployAppModuleVo> returnAppModuleVoList = new ArrayList<>();
        Long appSystemId = paramObj.getLong("appSystemId");
        Long envId = paramObj.getLong("envId");
        Long scenarioId = paramObj.getLong("scenarioId");
        returnAppModuleVoList = deployAppConfigMapper.getAppModuleListBySystemIdAndEnvId(appSystemId, envId);
        if (CollectionUtils.isNotEmpty(returnAppModuleVoList)) {
            //判断是否有配流水线
//            List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
            int count = deployAppConfigMapper.getAppConfigCountByAppSystemId(appSystemId);
            if (count == 0) {
                throw new DeployAppConfigNotFoundException(appSystemId);
            }
            //查询拥有runner的模块id列表
            List<Long> hasRunnerAppModuleIdList = deployAppConfigMapper.getAppModuleIdListHasRunnerByAppSystemIdAndModuleIdList(appSystemId, returnAppModuleVoList.stream().map(DeployAppModuleVo::getId).collect(Collectors.toList()));
            /*补充当前模块是否有BUILD分类的工具，前端需要根据此标识(isHasBuildTypeTool) 调用不同的选择版本下拉接口*/
            //1、获取流水线
//            Map<String, DeployAppConfigVo> appConfigVoMap = appConfigVoList.stream().collect(Collectors.toMap(o -> o.getAppSystemId().toString() + "-" + o.getAppModuleId().toString() + "-" + o.getEnvId().toString(), e -> e));
            for (DeployAppModuleVo appModuleVo : returnAppModuleVoList) {
//                DeployAppConfigVo configVo = appConfigVoMap.get(appSystemId.toString() + "-" + appModuleVo.getId().toString() + "-" + envId.toString());
//                if (configVo == null) {
//                    configVo = appConfigVoMap.get(appSystemId + "-" + appModuleVo.getId().toString() + "-0");
//                }
//                if (configVo == null) {
//                    configVo = appConfigVoMap.get(appSystemId + "-0" + "-0");
//                }
//                if (configVo == null) {
//                    throw new DeployAppConfigNotFoundException(appSystemId);
//                }
                DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId)
                        .withAppModuleId(appModuleVo.getId())
                        .withEnvId(envId)
                        .isHasBuildOrDeployTypeTool(true)
                        .isUpdateConfig(false)
                        .getConfig();
                if (pipelineConfigVo == null) {
                    throw new DeployAppConfigNotFoundException(appSystemId);
                }

                //2、找出当前流水线场景=scenarioId下的所有phaseNameList
                if (CollectionUtils.isEmpty(pipelineConfigVo.getScenarioList())) {
                    throw new DeployAppConfigScenarioNotFoundException();
                }
                AutoexecCombopScenarioVo scenarioVo = new AutoexecCombopScenarioVo();
                Optional<AutoexecCombopScenarioVo> optional = pipelineConfigVo.getScenarioList().stream().filter(e -> Objects.equals(scenarioId, e.getScenarioId())).findFirst();
                if (optional.isPresent()) {
                    scenarioVo = optional.get();
                }
                if (CollectionUtils.isEmpty(scenarioVo.getCombopPhaseNameList())) {
                    throw new DeployAppConfigScenarioPhaseNameListNotFoundException();
                }

                //3、判断场景的阶段列表是否有BUILD分类的工具
                appModuleVo.setIsHasDeployTypeTool(scenarioVo.getIsHasDeployTypeTool());
                appModuleVo.setIsHasBuildTypeTool(scenarioVo.getIsHasBuildTypeTool());
//                for (DeployPipelinePhaseVo pipelinePhaseVo : pipelineConfigVo.getCombopPhaseList()) {
//                    if (scenarioVo.getCombopPhaseNameList().contains(pipelinePhaseVo.getName())) {
//                        List<AutoexecCombopPhaseOperationVo> phaseOperationList = pipelinePhaseVo.getConfig().getPhaseOperationList();
//                        for (AutoexecCombopPhaseOperationVo operationVo : phaseOperationList) {
//                            if (StringUtils.equals(ToolType.TOOL.getValue(), operationVo.getOperationType())) {
//                                AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecServiceCrossoverService.getAutoexecOperationBaseVoByIdAndType(pipelinePhaseVo.getName(), operationVo, false);
//                                if (autoexecOperationBaseVo != null && StringUtils.equals(autoexecOperationBaseVo.getTypeName(), "BUILD")) {
//                                    appModuleVo.setIsHasBuildTypeTool(1);
//                                }
//                                if (autoexecOperationBaseVo != null && StringUtils.equals(autoexecOperationBaseVo.getTypeName(), "DEPLOY")) {
//                                    appModuleVo.setIsHasDeployTypeTool(1);
//                                }
//                            }
//                        }
//                        if (appModuleVo.getIsHasBuildTypeTool() == 1 && appModuleVo.getIsHasDeployTypeTool() == 1) {
//                            break;
//                        }
//                    }
//                }

                //4、查询模块是否已有配置好的runner
                if (hasRunnerAppModuleIdList.contains(appModuleVo.getId())) {
                    appModuleVo.setIsHasRunner(1);
                }
            }
        }

        return returnAppModuleVoList;
    }
}



