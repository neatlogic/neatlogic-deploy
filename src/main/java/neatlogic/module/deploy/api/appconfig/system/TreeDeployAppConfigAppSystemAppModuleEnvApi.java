package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TreeDeployAppConfigAppSystemAppModuleEnvApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/appmodule/env/tree";
    }

    @Override
    public String getName() {
        return "nmdaas.treedeployappconfigappsystemappmoduleenvapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid")
    })
    @Output({
            @Param(explode = DeployAppSystemVo[].class)
    })
    @Description(desc = "nmdaas.treedeployappconfigappsystemappmoduleenvapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        DeployAppSystemVo deployAppSystemVo = deployAppConfigMapper.getAppSystemById(appSystemId);
        if (deployAppSystemVo == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        List<DeployAppModuleVo> deployAppModuleList = deployAppConfigMapper.getAppModuleListBySystemId(appSystemId);
        deployAppSystemVo.setAppModuleList(deployAppModuleList);
        for (DeployAppModuleVo deployAppModuleVo : deployAppModuleList) {
            //查找发布的环境
            List<DeployAppEnvironmentVo> deployEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, deployAppModuleVo.getId());
            //查找cmdb的环境
            List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbEnvListByAppSystemIdAndModuleId(appSystemId, deployAppModuleVo.getId());
            Set<Long> envIdSet = new HashSet<>();
            List<DeployAppEnvironmentVo> envList = new ArrayList<>();
            for (DeployAppEnvironmentVo env : deployEnvList) {
                if (!envIdSet.contains(env.getId())) {
                    envList.add(env);
                    envIdSet.add(env.getId());
                }
            }
            for (DeployAppEnvironmentVo env : cmdbEnvList) {
                if (!envIdSet.contains(env.getId())) {
                    envList.add(env);
                    envIdSet.add(env.getId());
                }
            }
            deployAppModuleVo.setEnvList(envList);
        }
        List<DeployAppConfigVo> deployAppConfigList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        DeployPipelineConfigVo appPipelineConfigVo = getDeployPipelineConfigVo(deployAppConfigList, appSystemId, 0L, 0L);
        if (appPipelineConfigVo != null) {
            for (DeployAppModuleVo deployAppModuleVo : deployAppModuleList) {
                {
                    DeployPipelineConfigVo pipelineConfigVo = getDeployPipelineConfigVo(deployAppConfigList, appSystemId, deployAppModuleVo.getId(), 0L);
                    JSONObject resultObj = parsePipeline(appPipelineConfigVo, pipelineConfigVo);
                    deployAppModuleVo.setIsActive(resultObj.getInteger("isActive"));
                    deployAppModuleVo.setOverride(resultObj.getInteger("override"));
                }
                for (DeployAppEnvironmentVo env : deployAppModuleVo.getEnvList()) {
                    DeployPipelineConfigVo pipelineConfigVo = getDeployPipelineConfigVo(deployAppConfigList, appSystemId, deployAppModuleVo.getId(), env.getId());
                    JSONObject resultObj = parsePipeline(appPipelineConfigVo, pipelineConfigVo);
                    env.setIsActive(resultObj.getInteger("isActive"));
                    env.setOverride(resultObj.getInteger("override"));
                }
            }
        }
        return deployAppSystemVo;
    }

//    private JSONObject parsePipeline(Long appSystemId, Long appModuleId) {
//        return parsePipeline(appSystemId, appModuleId, null);
//    }
//
//    private JSONObject parsePipeline(Long appSystemId, Long appModuleId, Long envId) {
//        JSONObject resultObj = new JSONObject();
//        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(appSystemId)
//                .withAppModuleId(appModuleId)
//                .withEnvId(envId)
//                .getConfig();
//        if (deployPipelineConfigVo == null) {
//            return resultObj;
//        }
//        Integer override = 0;
//        Integer isActive = 1;
//        List<DeployPipelinePhaseVo> combopPhaseList = deployPipelineConfigVo.getCombopPhaseList();
//        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
//            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
//                if (Objects.equals(deployPipelinePhaseVo.getOverride(), 1)) {
//                    override = 1;
//                    break;
//                }
//            }
//            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
//                if (!Objects.equals(deployPipelinePhaseVo.getIsActive(), 1)) {
//                    isActive = 0;
//                    break;
//                }
//            }
//        }
//        resultObj.put("override", override);
//        resultObj.put("isActive", isActive);
//        return resultObj;
//    }

    private JSONObject parsePipeline(DeployPipelineConfigVo appPipelineConfigVo, DeployPipelineConfigVo deployPipelineConfigVo) {
        JSONObject resultObj = new JSONObject();
        int override = 0;
        int isActive = 1;
        Set<Long> appCombopPhaseIdSet = new HashSet<>();
        List<DeployPipelinePhaseVo> appCombopPhaseList = appPipelineConfigVo.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(appCombopPhaseList)) {
            appCombopPhaseIdSet = appCombopPhaseList.stream().map(DeployPipelinePhaseVo::getId).collect(Collectors.toSet());
        }
        if (deployPipelineConfigVo != null) {
            List<DeployPipelinePhaseVo> combopPhaseList = deployPipelineConfigVo.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                    if (appCombopPhaseIdSet.contains(deployPipelinePhaseVo.getId()) && Objects.equals(deployPipelinePhaseVo.getOverride(), 1)) {
                        override = 1;
                        break;
                    }
                }
//            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
//                if (appCombopPhaseUuidSet.contains(deployPipelinePhaseVo.getUuid()) && !Objects.equals(deployPipelinePhaseVo.getIsActive(), 1)) {
//                    isActive = 0;
//                    break;
//                }
//            }
            }
            List<Long> disabledPhaseIdList = deployPipelineConfigVo.getDisabledPhaseIdList();
            if (CollectionUtils.isNotEmpty(disabledPhaseIdList)) {
                for (Long disabledPhaseId : disabledPhaseIdList) {
                    if (appCombopPhaseIdSet.contains(disabledPhaseId)) {
                        isActive = 0;
                        break;
                    }
                }
            }
        }
        resultObj.put("override", override);
        resultObj.put("isActive", isActive);
        return resultObj;
    }

    private DeployPipelineConfigVo getDeployPipelineConfigVo(List<DeployAppConfigVo> deployAppConfigList, Long appSystemId, Long appModuleId, Long envId) {
        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
                    && Objects.equals(deployAppConfigVo.getAppModuleId(), appModuleId)
                    && Objects.equals(deployAppConfigVo.getEnvId(), envId)) {
                return deployAppConfigVo.getConfig();
            }
        }
//        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
//            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
//                    && Objects.equals(deployAppConfigVo.getAppModuleId(), appModuleId)
//                    && Objects.equals(deployAppConfigVo.getEnvId(), 0L)) {
//                return deployAppConfigVo.getConfig();
//            }
//        }
//        for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
//            if (Objects.equals(deployAppConfigVo.getAppSystemId(), appSystemId)
//                    && Objects.equals(deployAppConfigVo.getAppModuleId(), 0L)
//                    && Objects.equals(deployAppConfigVo.getEnvId(), 0L)) {
//                return deployAppConfigVo.getConfig();
//            }
//        }
        return null;
    }
}
