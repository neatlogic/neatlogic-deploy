/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.api.appconfig.module;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/8/23 16:06
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CopyDeployAppConfigModuleConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "fromAppModuleId", type = ApiParamType.LONG, isRequired = true, desc = "来源模块id"),
            @Param(name = "toAppModuleIdList", type = ApiParamType.JSONARRAY, desc = "目标模块id列表"),
            @Param(name = "abbrName", type = ApiParamType.STRING, desc = "简称(复制配置，并新建模块时使用)"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称(复制配置，并新建模块时使用)"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态(复制配置，并新建模块时使用)"),
            @Param(name = "ownerIdList", type = ApiParamType.JSONARRAY, desc = "负责人(复制配置，并新建模块时使用)"),
            @Param(name = "maintenanceWindow", type = ApiParamType.JSONARRAY, desc = "维护窗口(复制配置，并新建模块时使用)"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注(复制配置，并新建模块时使用)"),
            @Param(name = "isAdd", type = ApiParamType.INTEGER, isRequired = true, desc = "是否新增(0:现有，1:新增)")
    })
    @Output({
    })
    @Description(desc = "复制模块层配置(复制模块配置，以及来源模块下的环境配置)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        Long appSystemId = paramObj.getLong("appSystemId");
        Long fromAppModuleId = paramObj.getLong("fromAppModuleId");
        JSONArray toAppModuleIdArray = paramObj.getJSONArray("toAppModuleIdList");

        //获取当前系统下的所有配置 appConfigVoList
        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isEmpty(appConfigVoList)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        List<Long> fromModuleHasConfigEnvIdList = new ArrayList<>();
        Map<Long, List<DeployAppConfigVo>> appModuleIdConfigListMap = appConfigVoList.stream().collect(Collectors.groupingBy(DeployAppConfigVo::getAppModuleId));
        if (CollectionUtils.isNotEmpty(appModuleIdConfigListMap.get(fromAppModuleId))) {
            fromModuleHasConfigEnvIdList = appModuleIdConfigListMap.get(fromAppModuleId).stream().map(DeployAppConfigVo::getEnvId).collect(Collectors.toList());
        }

        if (paramObj.getInteger("isAdd") == 0) {
            List<Long> toAppModuleIdList = toAppModuleIdArray.toJavaList(Long.class);

            List<Long> allParamModuleIdList = new ArrayList<>();
            allParamModuleIdList.add(fromAppModuleId);
            allParamModuleIdList.addAll(toAppModuleIdList);
            List<DeployAppModuleEnvVo> appModuleEnvVoList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, allParamModuleIdList);
            Map<Long, List<Long>> appModuleEnvListMap = appModuleEnvVoList.stream().collect(Collectors.toMap(DeployAppModuleEnvVo::getId, DeployAppModuleEnvVo::getEnvIdList));
            List<Long> fromModuleEnvIdList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(fromAppModuleId))) {
                fromModuleEnvIdList = appModuleEnvListMap.get(fromAppModuleId);
            }

            List<DeployAppConfigVo> insertAppConfigList = new ArrayList<>();

            for (Long toModuleId : toAppModuleIdList) {

                //目标模块下有独一份配置的环境列表
                List<Long> toModuleHasConfigEnvIdList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(appModuleIdConfigListMap.get(toModuleId))) {
                    toModuleHasConfigEnvIdList = appModuleIdConfigListMap.get(toModuleId).stream().map(DeployAppConfigVo::getEnvId).collect(Collectors.toList());
                }

                //1、复制模块配置
                if (fromModuleHasConfigEnvIdList.contains(0L)) {
                    //新增模块配置，后面统一新增配置，insert时会duplicate
                    insertAppConfigList.add(new DeployAppConfigVo(appSystemId, toModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig()));
                } else if (toModuleHasConfigEnvIdList.contains(0L)) {
                    //删除原有的配置(虽然前端已经有接口控制只能选没有独一份配置的模块，但是防止单独调接口，做多一次删除动作)
                    deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toModuleId));
                }

                //2、复制模块下的环境配置
                //只有当来源模块拥有环境时，才需要复制环境相关配置
                if (CollectionUtils.isNotEmpty(fromModuleEnvIdList)) {
                    List<Long> needAddEnvIdList = new ArrayList<>();
                    List<Long> toModuleEnvIdList = new ArrayList<>();

                    //如果目标模块下有环境，则做差集，求出需要新增的环境
                    if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(toModuleId))) {
                        toModuleEnvIdList = appModuleEnvListMap.get(toModuleId);
                    }

                    for (Long fromEnvId : fromModuleEnvIdList) {

                        //如果目标模块下没有此环境，则新增
                        if (!toModuleEnvIdList.contains(fromEnvId)) {
                            needAddEnvIdList.add(fromEnvId);
                        }

                        //如果来源模块的环境有独一份配置，则复制
                        if (fromModuleHasConfigEnvIdList.contains(fromEnvId)) {
                            //新增配置，insert是会duplicate
                            insertAppConfigList.add(new DeployAppConfigVo(appSystemId, toModuleId, fromEnvId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).withEnvId(fromEnvId).getConfig()));
                        } else if (toModuleHasConfigEnvIdList.contains(fromEnvId)) {
                            //如果来源模块的当前环境没有独一份的配置，而目标模块的当前配置有独一份配置，则需要删除此配置
                            deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toModuleId, fromEnvId));
                        }

                        //删除dbConfig和autoConfig（有相同的环境才需要删除）
                        if (toModuleHasConfigEnvIdList.contains(fromEnvId)) {
                            deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, toModuleId, fromEnvId);
                            deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(appSystemId, toModuleId, fromEnvId));
                        }
                    }

                    if (CollectionUtils.isNotEmpty(needAddEnvIdList)) {
                        //给目标模块新增Deploy环境
                        deployAppConfigMapper.insertAppConfigEnv(appSystemId, toModuleId, needAddEnvIdList);
                    }

                    //复制环境层dbSchema和autoCfgKey配置
                    copyDbSchemaListAndAutoCfgKeyList(appSystemId, fromAppModuleId, toModuleId, fromModuleEnvIdList);
                }

                //复制模块层runnerGroup信息
                RunnerGroupVo fromModuleRunnerGroup = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId, fromAppModuleId);
                deployAppConfigMapper.deleteAppModuleRunnerGroup(new DeployAppConfigVo(appSystemId, toModuleId));
                if (fromModuleRunnerGroup != null) {
                    deployAppConfigMapper.insertAppModuleRunnerGroup(appSystemId, toModuleId, fromModuleRunnerGroup.getId());
                }
            }
            //新增或者update流水线配置
            if (CollectionUtils.isNotEmpty(insertAppConfigList)) {
                deployAppConfigMapper.insertBatchAppConfig(insertAppConfigList);
            }
        } else {

            Long toAppModuleId = deployAppConfigService.saveDeployAppModule(paramObj.toJavaObject(DeployAppModuleVo.class), 1);

            //复制配置
            List<DeployAppConfigVo> insertConfigList = new ArrayList<>();

            for (Long envId : fromModuleHasConfigEnvIdList) {
                if (envId == 0L) {
                    //模块层独有一份配置
                    insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig()));
                }
                //环境层独有一份配置
                insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, envId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).withEnvId(envId).getConfig()));
            }

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
            List<Long> envIdList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndModuleId(appSystemId, fromAppModuleId).stream().map(AppEnvironmentVo::getEnvId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(envIdList)) {
                deployAppConfigMapper.insertAppConfigEnv(appSystemId, toAppModuleId, envIdList);
                copyDbSchemaListAndAutoCfgKeyList(appSystemId, fromAppModuleId, toAppModuleId, envIdList);
            }
            return toAppModuleId;
        }
        return null;
    }

    /**
     * 复制dbSchemas和autoCfgKeys配置
     *
     * @param appSystemId     系统id
     * @param fromAppModuleId 来源模块id
     * @param toAppModuleId   目标模块id
     * @param envIdList       环境id列表
     */
    private void copyDbSchemaListAndAutoCfgKeyList(Long appSystemId, Long fromAppModuleId, Long toAppModuleId, List<Long> envIdList) {
        if (CollectionUtils.isEmpty(envIdList)) {
            return;
        }

        List<DeployAppEnvironmentVo> envInfoVoList = deployAppConfigMapper.getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, fromAppModuleId, envIdList);
        Map<Long, List<DeployAppConfigEnvDBConfigVo>> envDbSchemaListMap = new HashMap<>();
        Map<Long, List<DeployAppEnvAutoConfigKeyValueVo>> envAutoCfgKeyListMap = new HashMap<>();
        for (DeployAppEnvironmentVo envVo : envInfoVoList) {
            envDbSchemaListMap.put(envVo.getId(), envVo.getDbSchemaList());
            envAutoCfgKeyListMap.put(envVo.getId(), envVo.getAutoCfgKeyValueList());
        }
        List<DeployAppConfigEnvDBConfigVo> insertDbConfigVoList = new ArrayList<>();
        List<DeployAppEnvAutoConfigVo> insertAutoCfgVoList = new ArrayList<>();
        for (Long envId : envIdList) {
            List<DeployAppEnvAutoConfigKeyValueVo> insertAutoConfigKeyValueVoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(envDbSchemaListMap.get(envId))) {
                for (DeployAppConfigEnvDBConfigVo dbConfigVo : envDbSchemaListMap.get(envId)) {
                    insertDbConfigVoList.add(new DeployAppConfigEnvDBConfigVo(appSystemId, toAppModuleId, envId, dbConfigVo.getDbSchema(), dbConfigVo.getConfig()));
                }
            }
            if (CollectionUtils.isNotEmpty(envAutoCfgKeyListMap.get(envId))) {
                insertAutoConfigKeyValueVoList.addAll(envAutoCfgKeyListMap.get(envId));
            }
            if (CollectionUtils.isNotEmpty(insertAutoConfigKeyValueVoList)) {
                insertAutoCfgVoList.add(new DeployAppEnvAutoConfigVo(appSystemId, toAppModuleId, envId, insertAutoConfigKeyValueVoList));
            }
        }
        if (CollectionUtils.isNotEmpty(insertDbConfigVoList)) {
            deployAppConfigMapper.insertBatchAppConfigEnvDBConfig(insertDbConfigVoList);
        }
        if (CollectionUtils.isNotEmpty(insertAutoCfgVoList)) {
            deployAppConfigMapper.insertBatchAppEnvAutoConfig(insertAutoCfgVoList);
        }
    }
}
