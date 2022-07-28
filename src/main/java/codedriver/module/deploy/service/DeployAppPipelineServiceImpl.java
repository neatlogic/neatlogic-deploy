/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
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

//    /**
//     * 查询应用、模块、环境的流水线配置信息
//     * @param deployAppConfigVo
//     * @return
//     */
//    @Override
//    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo deployAppConfigVo) {
//        return getDeployPipelineConfigVo(deployAppConfigVo, null);
//    }

//    /**
//     * 查询应用、模块、环境的流水线配置信息
//     * @param deployAppConfigVo
//     * @param profileIdList
//     * @return
//     */
//    @Override
//    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo deployAppConfigVo, List<Long> profileIdList) {
//        Long appSystemId = deployAppConfigVo.getAppSystemId();
//        //查询应用层配置信息
//        String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
//        if (StringUtils.isBlank(configStr)) {
//            configStr = "{}";
//        }
//        DeployPipelineConfigVo config = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
//        overrideProfileParamSetSource(config.getOverrideProfileList(), "应用");
//        String targetLevel = "应用";
//        Long moduleId = deployAppConfigVo.getModuleId();
//        Long envId = deployAppConfigVo.getEnvId();
//        if (moduleId == 0L && envId == 0L) {
//
//        } else if (moduleId == 0L && envId != 0L) {
//            // 如果是访问环境层配置信息，moduleId不能为空
//            throw new ParamNotExistsException("moduleId");
//        } else {
//            //查询目标层配置信息
//            String overrideConfigStr = deployAppConfigMapper.getAppConfig(deployAppConfigVo);
//            if (StringUtils.isBlank(overrideConfigStr)) {
//                //如果查询不到目标层配置信息，说明没改动过，则返回上层配置信息
//                if (envId != 0L) {
//                    targetLevel = "环境";
//                    //目标层是环境层
//                    //查询模块层配置信息
//                    deployAppConfigVo.setEnvId(0L);
//                    String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfig(deployAppConfigVo);
//                    if (StringUtils.isBlank(moduleOverrideConfigStr)) {
//                        //如果查询不到模块层配置信息，说明没改动过，则返回应用层配置信息
//                        overridePhase(config.getCombopPhaseList());
//                    } else {
//                        //如果查询到模块层配置信息，说明有改动过，则返回应用层与模块层结合配置信息
//                        DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
//                        overridePhase(config.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList(), "模块");
//                        overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
//                        overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
//                        overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
//                    }
//                } else {
//                    targetLevel = "模块";
//                    //目标层是模块层
//                    overridePhase(config.getCombopPhaseList());
//                }
//            } else {
//                //如果查询到目标层配置信息，说明有改动过，则返回目标层与上层结合配置信息
//                if (envId != 0L) {
//                    targetLevel = "环境";
//                    //目标层是环境层
//                    DeployPipelineConfigVo envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
//                    //查询模块层配置信息
//                    deployAppConfigVo.setEnvId(0L);
//                    String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfig(deployAppConfigVo);
//                    if (StringUtils.isBlank(moduleOverrideConfigStr)) {
//                        //如果查询不到模块层配置信息，说明没改动过，则返回应用层与环境层结合配置信息
//                        overridePhase(config.getCombopPhaseList(), envOverrideConfig.getCombopPhaseList());
//                        overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
//                        overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "模块");
//                        overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
//                    } else {
//                        //如果查询到模块层配置信息，说明有改动过，则返回应用层与模块层结合配置信息
//                        DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
//                        List<DeployPipelinePhaseVo> appSystemCombopPhaseList = config.getCombopPhaseList();
//                        overridePhase(appSystemCombopPhaseList, moduleOverrideConfig.getCombopPhaseList(), "模块");
//                        overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
//                        overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
//                        overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
//
//                        overridePhase(appSystemCombopPhaseList, envOverrideConfig.getCombopPhaseList());
//                        overridePhaseGroup(config.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
//                        overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "模块");
//                        overrideProfile(config.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
//                    }
//                } else {
//                    //目标层是模块层
//                    targetLevel = "模块";
//                    DeployPipelineConfigVo moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
//                    overridePhase(config.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList());
//                    overridePhaseGroup(config.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
//                    overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
//                    overrideProfile(config.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
//                }
//            }
//        }
//
//        overrideProfileParamSetInherit(config.getOverrideProfileList(), targetLevel);
//        if (CollectionUtils.isEmpty(profileIdList)) {
//            profileIdList = new ArrayList<>(getProfileIdSet(config));
//        }
//        if (CollectionUtils.isNotEmpty(profileIdList)) {
//            IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
//            List<AutoexecProfileVo> profileList = autoexecProfileCrossoverService.getProfileVoListByIdList(profileIdList);
//            if (CollectionUtils.isNotEmpty(profileList)) {
//                List<DeployProfileVo> deployProfileList = getDeployProfileList(profileList);
//                List<DeployProfileVo> overrideProfileList = config.getOverrideProfileList();
//                finalOverrideProfile(deployProfileList, overrideProfileList);
//                config.setOverrideProfileList(deployProfileList);
//            }
//        }
//        return config;
//    }

    /**
     * 组装应用、模块、环境的流水线配置信息
     * @param appConfig
     * @param moduleOverrideConfig
     * @param envOverrideConfig
     * @param targetLevel
     * @return
     */
    @Override
    public DeployPipelineConfigVo mergeDeployPipelineConfigVo(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel) {
        return mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel, null);
    }

    /**
     * 组装应用、模块、环境的流水线配置信息
     * @param appConfig
     * @param moduleOverrideConfig
     * @param envOverrideConfig
     * @param targetLevel
     * @param profileIdList
     * @return
     */
    @Override
    public DeployPipelineConfigVo mergeDeployPipelineConfigVo(DeployPipelineConfigVo appConfig, DeployPipelineConfigVo moduleOverrideConfig, DeployPipelineConfigVo envOverrideConfig, String targetLevel, List<Long> profileIdList) {
        overrideProfileParamSetSource(appConfig.getOverrideProfileList(), "应用");
        if (moduleOverrideConfig == null && envOverrideConfig == null) {
            if (!Objects.equals(targetLevel, "应用")) {
                overridePhase(appConfig.getCombopPhaseList());
                appConfig.getExecuteConfigVo().setOverride(0);
            }
        } else if (moduleOverrideConfig != null && envOverrideConfig == null) {
            overrideExecuteConfig(appConfig.getExecuteConfigVo(), moduleOverrideConfig.getExecuteConfigVo());
            if (Objects.equals(targetLevel, "环境")) {
                overridePhase(appConfig.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList(), "模块");
                appConfig.getExecuteConfigVo().setOverride(0);
            } else {
                overridePhase(appConfig.getCombopPhaseList(), moduleOverrideConfig.getCombopPhaseList());
            }
            overridePhaseGroup(appConfig.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
            overrideProfile(appConfig.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());
        } else if (moduleOverrideConfig == null && envOverrideConfig != null) {
            overrideExecuteConfig(appConfig.getExecuteConfigVo(), envOverrideConfig.getExecuteConfigVo());
            overridePhase(appConfig.getCombopPhaseList(), envOverrideConfig.getCombopPhaseList());
            overridePhaseGroup(appConfig.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "环境");
            overrideProfile(appConfig.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
        } else if (moduleOverrideConfig != null && envOverrideConfig != null) {
            overrideExecuteConfig(appConfig.getExecuteConfigVo(), moduleOverrideConfig.getExecuteConfigVo());
            List<DeployPipelinePhaseVo> appSystemCombopPhaseList = appConfig.getCombopPhaseList();
            overridePhase(appSystemCombopPhaseList, moduleOverrideConfig.getCombopPhaseList(), "模块");
            overridePhaseGroup(appConfig.getCombopGroupList(), moduleOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(moduleOverrideConfig.getOverrideProfileList(), "模块");
            overrideProfile(appConfig.getOverrideProfileList(), moduleOverrideConfig.getOverrideProfileList());

            overrideExecuteConfig(appConfig.getExecuteConfigVo(), envOverrideConfig.getExecuteConfigVo());
            overridePhase(appSystemCombopPhaseList, envOverrideConfig.getCombopPhaseList());
            overridePhaseGroup(appConfig.getCombopGroupList(), envOverrideConfig.getCombopGroupList());
            overrideProfileParamSetSource(envOverrideConfig.getOverrideProfileList(), "环境");
            overrideProfile(appConfig.getOverrideProfileList(), envOverrideConfig.getOverrideProfileList());
        }

        overrideProfileParamSetInherit(appConfig.getOverrideProfileList(), targetLevel);
        if (CollectionUtils.isEmpty(profileIdList)) {
            profileIdList = new ArrayList<>(getProfileIdSet(appConfig));
        }
        if (CollectionUtils.isNotEmpty(profileIdList)) {
            IAutoexecProfileCrossoverService autoexecProfileCrossoverService = CrossoverServiceFactory.getApi(IAutoexecProfileCrossoverService.class);
            List<AutoexecProfileVo> profileList = autoexecProfileCrossoverService.getProfileVoListByIdList(profileIdList);
            if (CollectionUtils.isNotEmpty(profileList)) {
                List<DeployProfileVo> deployProfileList = getDeployProfileList(profileList);
                List<DeployProfileVo> overrideProfileList = appConfig.getOverrideProfileList();
                finalOverrideProfile(deployProfileList, overrideProfileList);
                appConfig.setOverrideProfileList(deployProfileList);
            }
        }
        return appConfig;
    }

    /**
     * 获取流水线配置信息
     * @param searchVo
     * @return
     */
    @Override
    public DeployPipelineConfigVo getDeployPipelineConfigVo(DeployAppConfigVo searchVo) {
        String targetLevel = null;
        DeployPipelineConfigVo appConfig = null;
        DeployPipelineConfigVo moduleOverrideConfig = null;
        DeployPipelineConfigVo envOverrideConfig = null;
        String overrideConfigStr = null;
        Long appSystemId = searchVo.getAppSystemId();
        Long appModuleId = searchVo.getAppModuleId();
        Long envId = searchVo.getEnvId();
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo != null) {
            overrideConfigStr = deployAppConfigVo.getConfigStr();
        }
        if (appModuleId == 0L && envId == 0L) {
            targetLevel = "应用";
            //查询应用层流水线配置信息
            if (StringUtils.isBlank(overrideConfigStr)) {
                throw new DeployAppConfigNotFoundException(appSystemId);
            }
            appConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
        } else if (appModuleId == 0L && envId != 0L) {
            // 如果是访问环境层配置信息，moduleId不能为空
            throw new ParamNotExistsException("moduleId");
        } else if (appModuleId != 0L && envId == 0L) {
            targetLevel = "模块";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            if (StringUtils.isNotBlank(overrideConfigStr)) {
                moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
            }
        } else {
            targetLevel = "环境";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId, appModuleId));
            if (StringUtils.isNotBlank(moduleOverrideConfigStr)) {
                moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
            }
            if (StringUtils.isNotBlank(overrideConfigStr)) {
                envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
            }
        }
        DeployPipelineConfigVo deployPipelineConfigVo = mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel);
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        autoexecServiceCrossoverService.updateAutoexecCombopConfig(deployPipelineConfigVo.getAutoexecCombopConfigVo());
        return deployPipelineConfigVo;
    }

    /**
     * 覆盖阶段列表配置信息
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, null);
    }

    /**
     * 覆盖阶段列表配置信息
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList 模块层或环境层阶段列表数据
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList) {
        overridePhase(appSystemCombopPhaseList, overrideCombopPhaseList, null);
    }
    /**
     * 覆盖阶段列表配置信息
     * @param appSystemCombopPhaseList 应用层阶段列表数据
     * @param overrideCombopPhaseList 模块层或环境层阶段列表数据
     * @param inheritName
     */
    private void overridePhase(List<DeployPipelinePhaseVo> appSystemCombopPhaseList, List<DeployPipelinePhaseVo> overrideCombopPhaseList, String inheritName) {
        if (CollectionUtils.isEmpty(appSystemCombopPhaseList)) {
            return;
        }
        for (DeployPipelinePhaseVo appSystemCombopPhaseVo : appSystemCombopPhaseList) {
            if (StringUtils.isBlank(appSystemCombopPhaseVo.getSource())) {
                appSystemCombopPhaseVo.setSource("应用");
            }
            if (appSystemCombopPhaseVo.getOverride() == null) {
                appSystemCombopPhaseVo.setOverride(0);
            }
            if (appSystemCombopPhaseVo.getIsActive() == null) {
                appSystemCombopPhaseVo.setIsActive(1);
            }
            if (CollectionUtils.isEmpty(overrideCombopPhaseList)) {
                continue;
            }
            for (DeployPipelinePhaseVo overrideCombopPhaseVo : overrideCombopPhaseList) {
                if (!Objects.equals(appSystemCombopPhaseVo.getName(), overrideCombopPhaseVo.getName())) {
                    continue;
                }
                if (Objects.equals(overrideCombopPhaseVo.getOverride(), 1)) {
                    if (StringUtils.isNotBlank(inheritName)) {
                        appSystemCombopPhaseVo.setSource(inheritName);
                        appSystemCombopPhaseVo.setOverride(0);
                    } else {
                        appSystemCombopPhaseVo.setOverride(1);
                    }
                    appSystemCombopPhaseVo.setConfig(overrideCombopPhaseVo.getConfig());
                } else {
                    AutoexecCombopPhaseConfigVo appSystemPhaseConfigVo = appSystemCombopPhaseVo.getConfig();
                    AutoexecCombopPhaseConfigVo overridePhaseConfigVo = overrideCombopPhaseVo.getConfig();
                    AutoexecCombopExecuteConfigVo executeConfigVo = overridePhaseConfigVo.getExecuteConfig();
                    if (executeConfigVo != null) {
                        appSystemPhaseConfigVo.setExecuteConfig(executeConfigVo);
                    }
                }
                Integer parentIsActive = appSystemCombopPhaseVo.getIsActive();
                appSystemCombopPhaseVo.setParentIsActive(parentIsActive);
                if (parentIsActive == 0) {
                    appSystemCombopPhaseVo.setIsActive(0);
                } else {
                    appSystemCombopPhaseVo.setIsActive(overrideCombopPhaseVo.getIsActive());
                }
                break;
            }
        }
    }

    /**
     * 覆盖阶段组列表配置信息
     * @param appSystemCombopGroupList 应用层阶段组列表数据
     * @param overrideCombopGroupList 模块层或环境层阶段组列表数据
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

    /**
     * 覆盖预置参数集列表数据
     * @param appSystemDeployProfileList 应用层预置参数集列表数据
     * @param overrideDeployProfileList 模块层或环境层预置参数集列表数据
     */
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

    /**
     * 覆盖预置参数集列表数据
     * @param appSystemDeployProfileParamList 应用层预置参数集列表数据
     * @param overrideDeployProfileParamList 模块层或环境层预置参数集列表数据
     */
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

    /**
     * 设置预置参数的inherit字段值
     * @param overrideDeployProfileList 预置参数集列表数据
     * @param targetLevel 目标层级
     */
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

    /**
     * 设置预置参数的source字段值
     * @param overrideDeployProfileList 预置参数集列表数据
     * @param source 来源名
     */
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

    /**
     * 获取流水线的阶段列表中引用预置参数集列表的profileId列表
     * @param config
     * @return
     */
    private Set<Long> getProfileIdSet(DeployPipelineConfigVo config) {
        Set<Long> profileIdSet = new HashSet<>();
        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return profileIdSet;
        }
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
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

    /**
     * 将AutoexecProfileVo列表转化成DeployProfileVo列表
     * @param profileList
     * @return
     */
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

    /**
     * 最终覆盖预置参数集列表数据
     * @param deployProfileList 原始的预置参数集列表
     * @param overrideDeployProfileList 流水线修改后的预置参数集列表
     */
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

    /**
     * 最终覆盖预置参数集列表数据
     * @param deployProfileParamList 原始的预置参数集列表
     * @param overrideDeployProfileParamList 流水线修改后的预置参数集列表
     */
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

    /**
     * 覆盖执行信息数据
     * @param appSystemExecuteConfigVo 应用层执行信息数据
     * @param overrideExecuteConfigVo 模块层或环境层执行信息数据
     */
    private void overrideExecuteConfig(DeployPipelineExecuteConfigVo appSystemExecuteConfigVo, DeployPipelineExecuteConfigVo overrideExecuteConfigVo) {
        Integer override = overrideExecuteConfigVo.getOverride();
        if (Objects.equals(override, 1)) {
            appSystemExecuteConfigVo.setOverride(override);
            appSystemExecuteConfigVo.setProtocolId(overrideExecuteConfigVo.getProtocolId());
            appSystemExecuteConfigVo.setExecuteUser(overrideExecuteConfigVo.getExecuteUser());
        }
    }
}
