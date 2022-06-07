/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopGroupVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigOverrideVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppPipelineGetApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID")
    })
    @Output({
            @Param(name = "Return", explode = DeployPipelineConfigVo.class, desc = "应用流水线配置信息")
    })
    @Description(desc = "获取应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigOverrideVo deployAppOverrideOverrideVo = paramObj.toJavaObject(DeployAppConfigOverrideVo.class);
        Long appSystemId = deployAppOverrideOverrideVo.getAppSystemId();
        String configStr = deployAppConfigMapper.getAppConfigByAppSystemId(appSystemId);
        if (StringUtils.isBlank(configStr)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        DeployPipelineConfigVo config = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
        Long moduleId = deployAppOverrideOverrideVo.getModuleId();
        Long envId = deployAppOverrideOverrideVo.getEnvId();
        if (moduleId == 0L && envId == 0L) {
            return config;
        }
        if (moduleId == 0L && envId != 0L) {
            throw new ParamNotExistsException("moduleId");
        }

        DeployAppConfigOverrideVo deployAppConfigOverrideVo = new DeployAppConfigOverrideVo();
        deployAppConfigOverrideVo.setAppSystemId(appSystemId);
        deployAppConfigOverrideVo.setModuleId(moduleId);
        deployAppConfigOverrideVo.setEnvId(envId);
        String overrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
        if (StringUtils.isNotBlank(overrideConfigStr)) {
            DeployPipelineConfigVo overrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
            List<AutoexecCombopGroupVo> combopGroupList = overrideConfig.getCombopGroupList();
            if (CollectionUtils.isNotEmpty(combopGroupList)) {
                config.setCombopGroupList(combopGroupList);
            }
            List<DeployPipelinePhaseVo> combopPhaseList = overrideConfig.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                config.setCombopPhaseList(combopPhaseList);
            }
            List<AutoexecProfileVo> overrideProfileList = overrideConfig.getOverrideProfileList();
            if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                config.setOverrideProfileList(overrideProfileList);
            }
        } else {
            if (envId != 0L) {
                deployAppConfigOverrideVo.setEnvId(0L);
                String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
                if (StringUtils.isNotBlank(moduleOverrideConfigStr)) {
                    DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
                    List<AutoexecCombopGroupVo> combopGroupList = moduleOverrideConfig.getCombopGroupList();
                    if (CollectionUtils.isNotEmpty(combopGroupList)) {
                        config.setCombopGroupList(combopGroupList);
                    }
                    List<DeployPipelinePhaseVo> combopPhaseList = moduleOverrideConfig.getCombopPhaseList();
                    if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                        for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                            deployPipelinePhaseVo.setInherit("模块");
                            deployPipelinePhaseVo.setIsActive(1);
                            deployPipelinePhaseVo.setOverride(0);
                        }
                        config.setCombopPhaseList(combopPhaseList);
                    }
                    List<AutoexecProfileVo> overrideProfileList = moduleOverrideConfig.getOverrideProfileList();
                    if (CollectionUtils.isNotEmpty(overrideProfileList)) {
                        config.setOverrideProfileList(overrideProfileList);
                    }
                } else {
                    List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                    if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                        for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                            deployPipelinePhaseVo.setInherit("应用");
                            deployPipelinePhaseVo.setIsActive(1);
                            deployPipelinePhaseVo.setOverride(0);
                        }
                    }
                }
            } else {
                List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                        deployPipelinePhaseVo.setInherit("应用");
                        deployPipelinePhaseVo.setIsActive(1);
                        deployPipelinePhaseVo.setOverride(0);
                    }
                }
            }
        }
        return config;
    }
}
