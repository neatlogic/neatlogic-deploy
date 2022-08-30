/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import codedriver.module.deploy.service.DeployAppConfigService;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
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
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "维护窗口(复制配置，并新建模块时使用)"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注(复制配置，并新建模块时使用)"),
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

        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isNotEmpty(toAppModuleIdArray)) {
            List<Long> toAppModuleIdList = toAppModuleIdArray.toJavaList(Long.class);
            if (CollectionUtils.isEmpty(appConfigVoList)) {
                return null;
            }

            for (Long toModuleId : toAppModuleIdList) {
                List<DeployAppConfigVo> insertConfigList = new ArrayList<>();
                //将有单独配置的模块、环境找出来
                boolean hasFromModuleConfig = false;
                boolean hasToModuleConfig = false;
                List<Long> fromModuleHasConfigEnvIdList = new ArrayList<>();
                List<Long> toModuleHasConfigEnvIdList = new ArrayList<>();
                for (DeployAppConfigVo appConfigVo : appConfigVoList) {
                    if (appConfigVo.getAppModuleId() == 0L) {
                        //应用层配置
                        continue;
                    }
                    if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
                        if (appConfigVo.getEnvId() == 0L) {
                            //模块层独有一份配置
                            hasFromModuleConfig = true;
                        } else {
                            //环境层独有一份配置
                            fromModuleHasConfigEnvIdList.add(appConfigVo.getEnvId());
                        }
                    } else if (Objects.equals(toModuleId, appConfigVo.getAppModuleId())) {
                        if (appConfigVo.getEnvId() == 0L) {
                            //模块层独有一份配置
                            hasToModuleConfig = true;
                        } else {
                            //环境层独有一份配置
                            toModuleHasConfigEnvIdList.add(appConfigVo.getEnvId());
                        }
                    }
                }

                //1、复制模块配置
                if (hasFromModuleConfig) {
                    //新增模块配置，后面统一新增配置，insert时会duplicate
                    insertConfigList.add(new DeployAppConfigVo(appSystemId, toModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig()));
                } else if (hasToModuleConfig) {
                    //删除全有的配置
                    deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toModuleId));
                }

                //2、复制模块下的环境配置
                List<DeployAppModuleEnvVo> appModuleEnvVoList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, Arrays.asList(fromAppModuleId, toModuleId), TenantContext.get().getDataDbName());
                Map<Long, List<Long>> appModuleEnvListMap = appModuleEnvVoList.stream().collect(Collectors.toMap(DeployAppModuleEnvVo::getId, DeployAppModuleEnvVo::getEnvIdList));
                List<Long> fromModuleEnvIdList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(fromAppModuleId))) {
                    fromModuleEnvIdList = appModuleEnvListMap.get(fromAppModuleId);
                }
                //只有于来源模块拥有环境，才需要复制环境相关配置
                if (CollectionUtils.isNotEmpty(fromModuleEnvIdList)) {
                    List<Long> needAddEnvIdList = new ArrayList<>();
                    List<Long> sameAddEnvIdList = new ArrayList<>();
                    List<Long> toModuleEnvIdList = new ArrayList<>();

                    //如果目标模块下有环境，则做差集，求出需要新增的环境
                    if (CollectionUtils.isNotEmpty(appModuleEnvListMap.get(toModuleId))) {
                        toModuleEnvIdList = appModuleEnvListMap.get(toModuleId);
                        List<Long> finalToModuleEnvIdList = toModuleEnvIdList;
                        needAddEnvIdList = fromModuleEnvIdList.stream().filter(e -> !finalToModuleEnvIdList.contains(e)).collect(Collectors.toList());
                        sameAddEnvIdList = fromModuleEnvIdList.stream().filter(finalToModuleEnvIdList::contains).collect(Collectors.toList());
                    } else {
                        needAddEnvIdList = fromModuleEnvIdList;
                    }

                    //循环需要新增的环境，在目标模块下新增环境
                    if (CollectionUtils.isNotEmpty(needAddEnvIdList)) {
                        //给目标模块新增Deploy环境
                        deployAppConfigMapper.insertAppConfigEnv(appSystemId, toModuleId, needAddEnvIdList);
                        for (Long needAddEnvId : needAddEnvIdList) {
                            //新增环境配置
                            if (fromModuleHasConfigEnvIdList.contains(needAddEnvId)) {
                                //新增环境配置，后面统一新增配置，insert时会duplicate
                                insertConfigList.add(new DeployAppConfigVo(appSystemId, toModuleId, needAddEnvId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).withEnvId(needAddEnvId).getConfig()));
                            }
                        }
                    }

                    //循环相同的环境
                    if (CollectionUtils.isNotEmpty(sameAddEnvIdList)) {
                        for (Long sameEnvId : sameAddEnvIdList) {
                            if (fromModuleHasConfigEnvIdList.contains(sameEnvId)) {
                                //新增配置，insert是会duplicate
                                insertConfigList.add(new DeployAppConfigVo(appSystemId, toModuleId, sameEnvId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).withEnvId(sameEnvId).getConfig()));
                            } else if (toModuleHasConfigEnvIdList.contains(sameEnvId)) {
                                //如果来源模块的当前环境没有独一份的配置，而目标模块的当前配置有独一份配置，则需要删除此配置
                                deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, toModuleId, sameEnvId));
                            }
                            //删除dbConfig和autoConfig（有相同的环境才需要删除）
                            deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, toModuleId, sameEnvId);
                            deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(appSystemId, toModuleId, sameEnvId));
                        }
                    }

                    //复制环境层dbSchema和autoCfgKey配置
                    List<Long> needAddEnvDBAndAutoCfgEnvIdList = new ArrayList<>();
                    needAddEnvDBAndAutoCfgEnvIdList.addAll(needAddEnvIdList);
                    needAddEnvDBAndAutoCfgEnvIdList.addAll(sameAddEnvIdList);
                    copyDbSchemaListAndAutoCfgKeyList(appSystemId, fromAppModuleId, toModuleId, needAddEnvDBAndAutoCfgEnvIdList);
                }

                //新增或者update流水线配置
                if (CollectionUtils.isNotEmpty(insertConfigList)) {
                    deployAppConfigMapper.insertBatchAppConfig(insertConfigList);
                }

                //复制模块层runnerGroup信息
                RunnerGroupVo fromModuleRunnerGroup = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId, fromAppModuleId);
                deployAppConfigMapper.deleteAppModuleRunnerGroup(new DeployAppConfigVo(appSystemId, toModuleId));
                if (fromModuleRunnerGroup != null) {
                    deployAppConfigMapper.insertAppModuleRunnerGroup(appSystemId, toModuleId, fromModuleRunnerGroup.getId());
                }
            }

        } else {

            /*新增应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            List<Long> stateIdList = new ArrayList<>();
            List<Long> ownerIdList = new ArrayList<>();
            //构建数据结构
            JSONArray stateIdArray = paramObj.getJSONArray("stateIdList");
            if (CollectionUtils.isNotEmpty(stateIdArray)) {
                stateIdList = stateIdArray.toJavaList(Long.class);
            }
            JSONArray ownerIdArray = paramObj.getJSONArray("ownerIdList");
            if (CollectionUtils.isNotEmpty(ownerIdArray)) {
                ownerIdList = ownerIdArray.toJavaList(Long.class);
            }
            paramObj.put("stateIdList", stateIdList);
            paramObj.put("ownerIdList", ownerIdList);

            //定义需要插入的字段
            paramObj.put("needUpdateAttrList", new JSONArray(Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description")));
            //获取应用系统的模型id
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo moduleCiVo = ciCrossoverMapper.getCiByName("APPComponent");
            paramObj.put("ciId", moduleCiVo.getId());
            paramObj.put("needUpdateRelList", new JSONArray(Collections.singletonList("APP")));
            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());

            //3、保存系统（配置项）
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            ciEntityTransactionList.add(ciEntityTransactionVo);
            ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            ciEntityService.saveCiEntity(ciEntityTransactionList);

            Long toAppModuleId = ciEntityTransactionVo.getCiEntityId();

            //复制配置

            List<DeployAppConfigVo> insertConfigList = new ArrayList<>();
            //将有单独配置找出来 insertConfigList
            for (DeployAppConfigVo appConfigVo : appConfigVoList) {
                if (appConfigVo.getAppModuleId() == 0L) {
                    //应用层配置
                    continue;
                }
                if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
                    if (appConfigVo.getEnvId() == 0L) {
                        //模块层独有一份配置
                        insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig()));
                    } else {
                        //环境层独有一份配置
                        insertConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId, appConfigVo.getEnvId(), DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).withEnvId(appConfigVo.getEnvId()).getConfig()));
                    }
                }
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
            List<Long> envIdList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemIdAndModuleId(appSystemId, fromAppModuleId, TenantContext.get().getDataDbName()).stream().map(AppEnvironmentVo::getEnvId).collect(Collectors.toList());
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

        List<DeployAppEnvironmentVo> envInfoVoList = deployAppConfigMapper.getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, fromAppModuleId, envIdList, TenantContext.get().getDataDbName());
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
