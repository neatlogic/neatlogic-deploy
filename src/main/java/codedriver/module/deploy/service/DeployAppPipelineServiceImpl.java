/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopGroupVo;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class DeployAppPipelineServiceImpl implements DeployAppPipelineService {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigOverrideVo deployAppConfigOverrideVo) {
        Long appSystemId = deployAppConfigOverrideVo.getAppSystemId();
        //查询应用层配置信息
        String configStr = deployAppConfigMapper.getAppConfigByAppSystemId(appSystemId);
        if (StringUtils.isBlank(configStr)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        DeployPipelineConfigVo config = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
        Long moduleId = deployAppConfigOverrideVo.getModuleId();
        Long envId = deployAppConfigOverrideVo.getEnvId();
        if (moduleId == 0L && envId == 0L) {
            return config;
        }
        // 如果是访问环境层配置信息，moduleId不能为空
        if (moduleId == 0L && envId != 0L) {
            throw new ParamNotExistsException("moduleId");
        }
        //查询目标层配置信息
        String overrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
        if (StringUtils.isBlank(overrideConfigStr)) {
            //如果查询不到目标层配置信息，说明没改动过，则返回上层配置信息
            if (envId != 0L) {
                //目标层是环境层
                //查询模块层配置信息
                deployAppConfigOverrideVo.setEnvId(0L);
                String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
                if (StringUtils.isBlank(moduleOverrideConfigStr)) {
                    //如果查询不到模块层配置信息，说明没改动过，则返回应用层配置信息
                    overridePhase(config.getCombopPhaseList());
                } else {
                    //如果查询到模块层配置信息，说明有改动过，则返回应用层与模块层结合配置信息
                    DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
                    overridePhase(config.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList(), "模块");
                    overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
                }
            } else {
                //目标层是模块层
                overridePhase(config.getCombopPhaseList());
            }
        } else {
            //如果查询到目标层配置信息，说明有改动过，则返回目标层与上层结合配置信息
            if (envId != 0L) {
                //目标层是环境层
                DeployPipelineConfigVo envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
                //查询模块层配置信息
                deployAppConfigOverrideVo.setEnvId(0L);
                String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
                if (StringUtils.isBlank(moduleOverrideConfigStr)) {
                    //如果查询不到模块层配置信息，说明没改动过，则返回应用层与环境层结合配置信息
                    overridePhase(config.getCombopPhaseList(), envOverrideConfig.getCombopPhaseList());
                    overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
                    overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
                } else {
                    //如果查询到模块层配置信息，说明有改动过，则返回应用层与模块层结合配置信息
                    DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
                    List<DeployPipelinePhaseVo> appSystemCombopPhaseList = config.getCombopPhaseList();
                    overridePhase(appSystemCombopPhaseList, moduleOverrideConfig.getCombopPhaseList(), "模块");
                    overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
                    overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());

                    overridePhase(appSystemCombopPhaseList, envOverrideConfig.getCombopPhaseList());
                    overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
                    overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
                }
            } else {
                //目标层是模块层
                DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
                overridePhase(config.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList());
                overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
                overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
            }
        }

        String targetLevel = envId == 0L ? "模块" : "环境";
        overrideProfileParamSetInherit(config.getOverrideProfileList(), targetLevel);
        return config;
    }

    /**
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, null);
    }

    /**
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList 模块层或环境层阶段列表数据
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, overrideCombopPhaseList, null);
    }
    /**
     *
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList 模块层或环境层阶段列表数据
     * @param inheritName
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList, String inheritName) {
        if (CollectionUtils.isNotEmpty(appSystemCombopPhaseList)) {
            for (DeployPipelinePhaseVo appSystemCombopPhaseVo : appSystemCombopPhaseList) {
                if (StringUtils.isBlank(appSystemCombopPhaseVo.getInherit())) {
                    appSystemCombopPhaseVo.setInherit("应用");
                }
                if (appSystemCombopPhaseVo.getOverride() == null) {
                    appSystemCombopPhaseVo.setOverride(0);
                }
                if (appSystemCombopPhaseVo.getIsActive() == null) {
                    appSystemCombopPhaseVo.setIsActive(1);
                }
                if (CollectionUtils.isNotEmpty(overrideCombopPhaseList)) {
                    for (DeployPipelinePhaseVo overrideCombopPhaseVo : overrideCombopPhaseList) {
                        if (Objects.equals(appSystemCombopPhaseVo.getName(), overrideCombopPhaseVo.getName())) {
                            if (Objects.equals(overrideCombopPhaseVo.getOverride(), 1)) {
                                if (StringUtils.isNotBlank(inheritName)) {
                                    appSystemCombopPhaseVo.setInherit(inheritName);
                                    appSystemCombopPhaseVo.setOverride(0);
                                } else {
                                    appSystemCombopPhaseVo.setOverride(1);
                                }

                                appSystemCombopPhaseVo.setIsActive(overrideCombopPhaseVo.getIsActive());
                                appSystemCombopPhaseVo.setConfig(overrideCombopPhaseVo.getConfigStr());
//                            appSystemCombopPhaseVo.setExecMode(overrideCombopPhaseVo.getExecMode());
//                            appSystemCombopPhaseVo.setExecModeName(overrideCombopPhaseVo.getExecModeName());
                            } else if (Objects.equals(overrideCombopPhaseVo.getIsActive(), 0)) {
                                appSystemCombopPhaseVo.setIsActive(0);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param appSystemCombopGroupList
     * @param overrideCombopGroupList
     */
    private void overridePhaseGroup(List<AutoexecCombopGroupVo> appSystemCombopGroupList, List<AutoexecCombopGroupVo> overrideCombopGroupList ) {
        if (CollectionUtils.isNotEmpty(appSystemCombopGroupList) && CollectionUtils.isNotEmpty(overrideCombopGroupList)) {
            for (AutoexecCombopGroupVo appSystemCombopGroup : appSystemCombopGroupList) {
                for (AutoexecCombopGroupVo overrideCombopGroup : overrideCombopGroupList) {
                    if (Objects.equals(appSystemCombopGroup.getUuid(), overrideCombopGroup.getUuid())) {
                        appSystemCombopGroup.setPolicy(overrideCombopGroup.getPolicy());
                        appSystemCombopGroup.setConfig(overrideCombopGroup.getConfigStr());
                    }
                }
            }
        }
    }

    private void overrideProfile(List<DeployProfileVo> appSystemDeployProfileList, List<DeployProfileVo> overrideDeployProfileList) {
        if ( CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        if (CollectionUtils.isEmpty(appSystemDeployProfileList)) {
            appSystemDeployProfileList.addAll(overrideDeployProfileList);
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            boolean flag = true;
            for (DeployProfileVo appSystemDeployProfile : appSystemDeployProfileList) {
                if (Objects.equals(appSystemDeployProfile.getProfileId(), overrideDeployProfile.getProfileId())) {
                    flag = false;
                    List<DeployProfileParamVo> appSystemDeployProfileParamList = appSystemDeployProfile.getParamList();
                    List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
                    overrideProfileParam(appSystemDeployProfileParamList, overrideDeployProfileParamList);
                    break;
                }
            }
            if (flag) {
                appSystemDeployProfileList.add(overrideDeployProfile);
            }
        }
    }

    private void overrideProfileParam(List<DeployProfileParamVo> appSystemDeployProfileParamList, List<DeployProfileParamVo> overrideDeployProfileParamList) {
        if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
            return;
        }
        if (CollectionUtils.isEmpty(appSystemDeployProfileParamList)) {
            appSystemDeployProfileParamList.addAll(overrideDeployProfileParamList);
            return;
        }
        for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
            boolean flag = true;
            for (DeployProfileParamVo appSystemDeployProfileParam : appSystemDeployProfileParamList) {
                if (Objects.equals(appSystemDeployProfileParam.getKey(), overrideDeployProfileParam.getKey())) {
                    flag = false;
                    if (overrideDeployProfileParam.getInherit() == 1) {
                        continue;
                    }
//                    appSystemDeployProfileParam.setInherit(overrideDeployProfileParam.getInherit());
                    appSystemDeployProfileParam.setSource(overrideDeployProfileParam.getSource());
                    appSystemDeployProfileParam.setValue(overrideDeployProfileParam.getValue());
                    break;
                }
            }
            if (flag) {
                appSystemDeployProfileParamList.add(overrideDeployProfileParam);
            }
        }
    }

    private void overrideProfileParamSetInherit(List<DeployProfileVo> overrideDeployProfileList, String targetLevel) {
        if ( CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
            if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
                continue;
            }
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (Objects.equals(overrideDeployProfileParam.getSource(), targetLevel)) {
                    overrideDeployProfileParam.setInherit(0);
                } else {
                    overrideDeployProfileParam.setInherit(1);
                }
            }
        }
    }
}
