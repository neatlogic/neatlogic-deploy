package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        return "查询发布应用配置的应用系统模块环境树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID")
    })
    @Output({
            @Param(explode = DeployAppSystemVo[].class)
    })
    @Description(desc = "查询发布应用配置的应用系统模块环境树")
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
            JSONObject resultObj = parsePipeline(appSystemId, deployAppModuleVo.getId());
            deployAppModuleVo.setIsActive(resultObj.getInteger("isActive"));
            deployAppModuleVo.setOverride(resultObj.getInteger("override"));
            //查找发布的环境
            List<DeployAppEnvironmentVo> deployEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, deployAppModuleVo.getId());
            //查找cmdb的环境
            List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbEnvListByAppSystemIdAndModuleId(appSystemId, deployAppModuleVo.getId());
            List<DeployAppEnvironmentVo> envList = new ArrayList<>();
            envList.addAll(deployEnvList);
            envList.addAll(cmdbEnvList);
            deployAppModuleVo.setEnvList(envList);
            for (DeployAppEnvironmentVo env : envList) {
                resultObj = parsePipeline(appSystemId, deployAppModuleVo.getId(), env.getId());
                env.setIsActive(resultObj.getInteger("isActive"));
                env.setOverride(resultObj.getInteger("override"));
            }
        }
        return deployAppSystemVo;
    }

    private JSONObject parsePipeline(Long appSystemId, Long appModuleId) {
        return parsePipeline(appSystemId, appModuleId, null);
    }

    private JSONObject parsePipeline(Long appSystemId, Long appModuleId, Long envId) {
        JSONObject resultObj = new JSONObject();
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(appSystemId)
                .withAppModuleId(appModuleId)
                .withEnvId(envId)
                .getConfig();
        if (deployPipelineConfigVo == null) {
            return resultObj;
        }
        Integer override = 0;
        Integer isActive = 1;
        List<DeployPipelinePhaseVo> combopPhaseList = deployPipelineConfigVo.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                if (Objects.equals(deployPipelinePhaseVo.getOverride(), 1)) {
                    override = 1;
                    break;
                }
            }
            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                if (!Objects.equals(deployPipelinePhaseVo.getIsActive(), 1)) {
                    isActive = 0;
                    break;
                }
            }
        }
        resultObj.put("override", override);
        resultObj.put("isActive", isActive);
        return resultObj;
    }
}
