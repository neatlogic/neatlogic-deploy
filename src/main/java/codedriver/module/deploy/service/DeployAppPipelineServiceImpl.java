/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class DeployAppPipelineServiceImpl implements DeployAppPipelineService {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigOverrideVo deployAppConfigOverrideVo) {
        return getDeployPipelineConfigVo(deployAppConfigOverrideVo, null);
    }

    @Override
    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigOverrideVo deployAppConfigOverrideVo, List<Long> profileIdList) {
        Long appSystemId = deployAppConfigOverrideVo.getAppSystemId();
        //查询应用层配置信息
        String configStr = deployAppConfigMapper.getAppConfigByAppSystemId(appSystemId);
        if (StringUtils.isBlank(configStr)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        DeployPipelineConfigVo config = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
        overrideProfileParamSetSource(config.getOverrideProfileList(), "应用");
        String targetLevel = "应用";
        Long moduleId = deployAppConfigOverrideVo.getModuleId();
        Long envId = deployAppConfigOverrideVo.getEnvId();
        if (moduleId == 0L && envId == 0L) {

        } else if (moduleId == 0L && envId != 0L) {
            // 如果是访问环境层配置信息，moduleId不能为空
            throw new ParamNotExistsException("moduleId");
        } else {
            //查询目标层配置信息
            String overrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
            if (StringUtils.isBlank(overrideConfigStr)) {
                //如果查询不到目标层配置信息，说明没改动过，则返回上层配置信息
                if (envId != 0L) {
                    targetLevel = "环境";
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
                        overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
                        overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
                    }
                } else {
                    targetLevel = "模块";
                    //目标层是模块层
                    overridePhase(config.getCombopPhaseList());
                }
            } else {
                //如果查询到目标层配置信息，说明有改动过，则返回目标层与上层结合配置信息
                if (envId != 0L) {
                    targetLevel = "环境";
                    //目标层是环境层
                    DeployPipelineConfigVo envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
                    //查询模块层配置信息
                    deployAppConfigOverrideVo.setEnvId(0L);
                    String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfigOverrideConfig(deployAppConfigOverrideVo);
                    if (StringUtils.isBlank(moduleOverrideConfigStr)) {
                        //如果查询不到模块层配置信息，说明没改动过，则返回应用层与环境层结合配置信息
                        overridePhase(config.getCombopPhaseList(), envOverrideConfig.getCombopPhaseList());
                        overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
                        overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "模块");
                        overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
                    } else {
                        //如果查询到模块层配置信息，说明有改动过，则返回应用层与模块层结合配置信息
                        DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
                        List<DeployPipelinePhaseVo> appSystemCombopPhaseList = config.getCombopPhaseList();
                        overridePhase(appSystemCombopPhaseList, moduleOverrideConfig.getCombopPhaseList(), "模块");
                        overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
                        overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
                        overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());

                        overridePhase(appSystemCombopPhaseList, envOverrideConfig.getCombopPhaseList());
                        overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
                        overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "模块");
                        overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
                    }
                } else {
                    //目标层是模块层
                    targetLevel = "模块";
                    DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
                    overridePhase(config.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList());
                    overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
                    overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
                    overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
                }
            }
        }


//        String targetLevel = envId == 0L ? "模块" : "环境";
        overrideProfileParamSetInherit(config.getOverrideProfileList(), targetLevel);
        if (CollectionUtils.isEmpty(profileIdList)) {
            profileIdList = new ArrayList<>(getProfileIdSet(config));
        }
        if (CollectionUtils.isNotEmpty(profileIdList)) {
            IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
            List<AutoexecProfileVo> profileList = autoexecProfileCrossoverService.getProfileVoListByIdList(profileIdList);
            if (CollectionUtils.isNotEmpty(profileList)) {
                List<DeployProfileVo> deployProfileList = getDeployProfileList(profileList);
                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
                finalOverrideProfile(deployProfileList, overrideProfileList);
            }
        }
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
                    appSystemDeployProfileParam.setDefaultValue(overrideDeployProfileParam.getDefaultValue());
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

    private void overrideProfileParamSetSource(List<DeployProfileVo> overrideDeployProfileList, String source) {
        if ( CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
            List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
            if (CollectionUtils.isEmpty(overrideDeployProfileParamList)) {
                continue;
            }
            for (DeployProfileParamVo overrideDeployProfileParam : overrideDeployProfileParamList) {
                if (Objects.equals(overrideDeployProfileParam.getInherit(), 0)) {
                    overrideDeployProfileParam.setSource(source);
                }
            }
        }
    }
    private Set<Long> getProfileIdSet(DeployPipelineConfigVo config) {
        Set<Long> profileIdSet = new HashSet<>();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return profileIdSet;
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
        return profileIdSet;
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
                    DeployProfileParamVo deployProfileParamVo = new DeployProfileParamVo(autoexecProfileParamVo);
//                    deployProfileParamVo.setKey(autoexecProfileParamVo.getKey());
//                    deployProfileParamVo.setDefaultValue(autoexecProfileParamVo.getDefaultValue());
//                    deployProfileParamVo.setDescription(autoexecProfileParamVo.getDescription());
//                    deployProfileParamVo.setType(autoexecProfileParamVo.getType());
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

    private void finalOverrideProfile(List<DeployProfileVo> deployProfileList, List<DeployProfileVo> overrideDeployProfileList) {
        if ( CollectionUtils.isEmpty(overrideDeployProfileList)) {
            return;
        }
        for (DeployProfileVo deployProfile : deployProfileList) {
            for (DeployProfileVo overrideDeployProfile : overrideDeployProfileList) {
                if (Objects.equals(deployProfile.getProfileId(), overrideDeployProfile.getProfileId())) {
                    List<DeployProfileParamVo> deployProfileParamList = deployProfile.getParamList();
                    List<DeployProfileParamVo> overrideDeployProfileParamList = overrideDeployProfile.getParamList();
                    finalOverrideProfileParam(deployProfileParamList, overrideDeployProfileParamList);
                    break;
                }
            }
        }
    }

    private void finalOverrideProfileParam(List<DeployProfileParamVo> deployProfileParamList, List<DeployProfileParamVo> overrideDeployProfileParamList) {
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
                    deployProfileParam.setDefaultValue(overrideDeployProfileParam.getDefaultValue());
                    break;
                }
            }
        }
    }
}
