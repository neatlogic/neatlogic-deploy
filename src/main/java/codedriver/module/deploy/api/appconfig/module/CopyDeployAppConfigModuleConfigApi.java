/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.restful.annotation.*;
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
 * @date 2022/8/23 16:06
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CopyDeployAppConfigModuleConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "复制模块层配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/module/config/copy";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "fromAppModuleId", type = ApiParamType.LONG, desc = "来源模块id"),
            @Param(name = "toAppModuleId", type = ApiParamType.LONG, desc = "目标模块id")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "复制模块层配置")
    })
    @Description(desc = "复制模块层配置(复制模块配置，以及来源模块和目标模块拥有相同的环境的配置)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long appSystemId = paramObj.getLong("appSystemId");
        Long fromAppModuleId = paramObj.getLong("fromAppModuleId");
        Long toAppModuleId = paramObj.getLong("toAppModuleId");

        if (toAppModuleId != null) {
            /* 1、给目标模块挂上来源模块的环境
                2、复制模块配置和所有环境的配置*/

            List<DeployAppConfigVo> insertConfigList = new ArrayList<>();
            List<DeployAppConfigVo> updateConfigList = new ArrayList<>();
            List<DeployAppConfigVo> deleteConfigList = new ArrayList<>();


            List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
            if (CollectionUtils.isEmpty(appConfigVoList)) {
                return null;
            }
            boolean hasFromModuleConfig = false;
            boolean hasToModuleConfig = false;
            List<Long> fromModuleHasConfigEnvIdList = new ArrayList<>();
            List<Long> toModuleHasConfigEnvIdList = new ArrayList<>();
            for (DeployAppConfigVo appConfigVo : appConfigVoList) {
                if (appConfigVo.getAppModuleId() == 0L) {
                    //应用层配置
                    continue;
                }
//                if (appConfigVo.getEnvId() == 0L) {
//
//                    if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
//                        hasFromModuleConfig = true;
//                    } else if (Objects.equals(toAppModuleId, appConfigVo.getAppModuleId())) {
//                        hasToModuleConfig = true;
//                    }
//                } else if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
//
//                    hasConfigEnvIdList.add(appConfigVo.getEnvId());
//                }


                if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
                    if (appConfigVo.getEnvId() == 0L) {
                        //模块层独有一份配置
                        hasFromModuleConfig = true;
                    } else {
                        //环境层独有一份配置
                        fromModuleHasConfigEnvIdList.add(appConfigVo.getEnvId());
                    }
                } else if (Objects.equals(toAppModuleId, appConfigVo.getAppModuleId())) {
                    if (appConfigVo.getEnvId() == 0L) {
                        //模块层独有一份配置
                        hasToModuleConfig = true;
                    } else {
                        //环境层独有一份配置
                        toModuleHasConfigEnvIdList.add(appConfigVo.getEnvId());
                    }
                }
            }


            //复制模块配置
            if (hasFromModuleConfig) {
                DeployAppConfigVo toModuleConfigVo = new DeployAppConfigVo(appSystemId, toAppModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig());
                insertConfigList.add(toModuleConfigVo);
            } else if (hasToModuleConfig) {
                deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toAppModuleId));
            }


            //复制模块下的环境配置
            List<DeployAppModuleEnvVo> appModuleEnvVoList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, Arrays.asList(fromAppModuleId, toAppModuleId), TenantContext.get().getDataDbName());
            Map<Long, List<AppEnvironmentVo>> appModuleEnvListMap = appModuleEnvVoList.stream().collect(Collectors.toMap(DeployAppModuleEnvVo::getId, DeployAppModuleEnvVo::getEnvList));
            List<Long> fromModuleEnvIdList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(fromAppModuleId))) {
                fromModuleEnvIdList = appModuleEnvListMap.get(fromAppModuleId).stream().map(AppEnvironmentVo::getEnvId).collect(Collectors.toList());
            }
            //只有于来源模块拥有环境，才需要复制环境相关配置
            if (CollectionUtils.isNotEmpty(fromModuleEnvIdList)) {
                List<Long> needAddEnvIdList = new ArrayList<>();
                List<Long> sameAddEnvIdList = new ArrayList<>();
                List<Long> toModuleEnvIdList = new ArrayList<>();

                //如果目标模块下有环境，则做差集，求出需要新增的环境
                if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(toAppModuleId))) {
                    toModuleEnvIdList = appModuleEnvListMap.get(toAppModuleId).stream().map(AppEnvironmentVo::getEnvId).collect(Collectors.toList());
                    List<Long> finalToModuleEnvIdList = toModuleEnvIdList;
                    needAddEnvIdList = fromModuleEnvIdList.stream().filter(e -> !finalToModuleEnvIdList.contains(e)).collect(Collectors.toList());
                    sameAddEnvIdList = fromModuleEnvIdList.stream().filter(finalToModuleEnvIdList::contains).collect(Collectors.toList());
                } else {
                    needAddEnvIdList = fromModuleEnvIdList;
                }
                if (CollectionUtils.isNotEmpty(needAddEnvIdList)) {
                    //TODO 给目标模块新增环境

                    for (Long needAddEnvId : needAddEnvIdList) {
                        if (fromModuleHasConfigEnvIdList.contains(needAddEnvId)) {
                            //新增流水线配置
                            insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, needAddEnvId));
                        }
                    }
                }

                //TODO 可以和needAddEnvIdList 合并再循环
                if (CollectionUtils.isNotEmpty(sameAddEnvIdList)) {
                    for (Long sameEnvId : sameAddEnvIdList) {
                        if (fromModuleHasConfigEnvIdList.contains(sameEnvId)) {
                            //新增配置，insert是会duplicate
                            insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, sameEnvId));
                        } else if (toModuleHasConfigEnvIdList.contains(sameEnvId)) {
                            //如果来源模块的当前环境没有独一份的配置，而目标模块的当前配置有独一份配置，则需要删除此配置
                            deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toAppModuleId, sameEnvId));
                        }
                        //删除dbConfig和autoConfig
                        deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, toAppModuleId, sameEnvId);
                        deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(appSystemId, toAppModuleId, sameEnvId));
                    }
                }
            }







            /*复制模块流水线配置、模块层runnerGroup信息*/


            //新增或者update流水线配置
            if (CollectionUtils.isNotEmpty(insertConfigList)) {
                deployAppConfigMapper.insertBatchAppConfig(insertConfigList);
            }
            //复制模块层runnerGroup信息
            RunnerGroupVo fromModuleRunnerGroup = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId, fromAppModuleId);
            deployAppConfigMapper.deleteAppModuleRunnerGroup(new DeployAppConfigVo(appSystemId, toAppModuleId));
            if (fromModuleRunnerGroup != null) {
                deployAppConfigMapper.insertAppModuleRunnerGroup(appSystemId, toAppModuleId, fromModuleRunnerGroup.getId());
            }


        } else {
            /*  1、新增目标模块，
                2、给目标模块挂上来源模块的环境
                3、复制模块配置和所有环境的配置*/


        }


        return null;
    }

    //TODO 复制dbSchemas和autoCfgKeys配置
    private void copyDbSchemaListAndAutoCfgKeyList(Long appSystemId, Long fromAppModuleId, Long toAppModuleId, List<Long> envIdList) {

        List<DeployAppEnvironmentVo> envVoListIncludeDbSchemaAndAutoCfgKey = deployAppConfigMapper.getAppConfigEnvDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, fromAppModuleId, envIdList);
        Map<Long, List<String>> envDbSchemaListMap = new HashMap<>();
        Map<Long, List<String>> envAutoCfgKeyListMap = new HashMap<>();
        for (DeployAppEnvironmentVo envVo: envVoListIncludeDbSchemaAndAutoCfgKey) {
            envDbSchemaListMap.put(envVo.getId(), envVo.getDbSchemaList());
            envAutoCfgKeyListMap.put(envVo.getId(), envVo.getAutoCfgKeyList());
        }
        List<DeployAppConfigEnvDBConfigVo> insertDbConfigVoList = new ArrayList<>();
        List<DeployAppEnvAutoConfigVo> insertAutoCfgVoList = new ArrayList<>();
        for (Long envId : envIdList) {
            List<DeployAppEnvAutoConfigKeyValueVo> insertAutoConfigKeyValueVoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(envDbSchemaListMap.get(envId))) {
                for (String dbSchema : envDbSchemaListMap.get(envId)) {
                    insertDbConfigVoList.add(new DeployAppConfigEnvDBConfigVo(appSystemId, toAppModuleId, envId,dbSchema));
                }
            }
            if (CollectionUtils.isNotEmpty(envAutoCfgKeyListMap.get(envId))) {
                for (String autoCfgKey : envAutoCfgKeyListMap.get(envId)) {
                    insertAutoConfigKeyValueVoList.add(new DeployAppEnvAutoConfigKeyValueVo(autoCfgKey));
                }
            }
            insertAutoCfgVoList.add(new DeployAppEnvAutoConfigVo(appSystemId,toAppModuleId,envId,insertAutoConfigKeyValueVoList));
        }
        deployAppConfigMapper.insertBatchAppConfigEnvDBConfig(insertDbConfigVoList);
        deployAppConfigMapper.insertBatchAppEnvAutoConfig(insertAutoCfgVoList);

    }

}
