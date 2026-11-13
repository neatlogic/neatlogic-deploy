/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvAttrVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigAuditVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyIrregularException;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyRepeatException;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyTypeIrregularException;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvDBSchemaNameRepeatException;
import neatlogic.framework.deploy.exception.DeployDatabaseEnvironmentConflictException;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在领域模块内维护共享校验；仅写入方法声明事务，预检异常由调用方决定返回或回滚。 */
@Service
public class DeployEnvironmentWriter {
    @Resource private DeployAppConfigMapper deployAppConfigMapper;
    @Resource private DeployAppConfigService deployAppConfigService;
    @Resource private DeployAppAuthorityService deployAppAuthorityService;

    /** 保存数据库 Schema，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveSchema(JSONObject paramObj) throws Exception {


        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(paramObj.getLong("envId"));
        if (env == null) {
            throw new AppEnvNotFoundException(paramObj.getLong("envId"));
        }
        CiEntityVo DBCiEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("dbResourceId"));
        if (DBCiEntityVo == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("dbResourceId"));
        }

        // 重新读取实际关联，防止选择候选后环境发生变化；必须先校验再写发布配置。
        checkDatabaseEnvironment(DBCiEntityVo, env);

        //保存DB发布配置
        DeployAppConfigEnvDBConfigVo dbConfigVo = paramObj.toJavaObject(DeployAppConfigEnvDBConfigVo.class);
        if (deployAppConfigMapper.checkDeployAppConfigEnvDBSchemaIsRepeat(dbConfigVo) > 0) {
            throw new DeployAppConfigEnvDBSchemaNameRepeatException(dbConfigVo.getDbSchema());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            deployAppConfigMapper.updateDeployAppConfigEnvDBConfig(dbConfigVo);
        } else {
            deployAppConfigMapper.insertAppConfigEnvDBConfig(dbConfigVo);
        }

        //保存DB配置项系统模块环境信息
        //添加环境属性、模块关系
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo DBCiEntityInfo = ciEntityService.getCiEntityById(DBCiEntityVo.getCiId(), DBCiEntityVo.getId());
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(DBCiEntityInfo);
        ciEntityTransactionVo.setCiId(DBCiEntityVo.getCiId());
        ciEntityTransactionVo.setAllowCommit(true);
        ciEntityTransactionVo.setDescription(null);
        JSONObject attrEntityData = DBCiEntityInfo.getAttrEntityData();
        if (MapUtils.isNotEmpty(attrEntityData)) {
            ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(attrEntityData.toJSONString()));
        }
        JSONObject relEntityData = DBCiEntityInfo.getRelEntityData();
        if (MapUtils.isNotEmpty(relEntityData)) {
            ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(relEntityData.toJSONString()));
        }
        JSONObject globalAttrEntityData = DBCiEntityInfo.getGlobalAttrEntityData();
        if (MapUtils.isNotEmpty(globalAttrEntityData)) {
            ciEntityTransactionVo.setGlobalAttrEntityData(JSONObject.parseObject(globalAttrEntityData.toJSONString()));
        }
        deployAppConfigService.addAttrEntityDataAndRelEntityData(
                ciEntityTransactionVo,
                DBCiEntityInfo.getCiId(),
                paramObj,
                Collections.singletonList("app_environment"),
                Collections.singletonList("APPComponent"),
                Collections.singletonList("app_environment")
        );
        //保存
        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);

        return null;
    }

    /** 按候选查询相同规则检查单选环境冲突，不覆盖数据库原有环境。 */
    void checkDatabaseEnvironment(CiEntityVo database, ResourceVo env) {
        List<ResourceVo> conflictingEnvs = deployAppConfigMapper.getDatabaseConflictingEnvList(database.getId(), env.getId());
        if (CollectionUtils.isNotEmpty(conflictingEnvs)) {
            String names = conflictingEnvs.stream().map(ResourceVo::getEnvName).collect(Collectors.joining(", "));
            String ids = conflictingEnvs.stream().map(item -> String.valueOf(item.getEnvId())).collect(Collectors.joining(", "));
            throw new DeployDatabaseEnvironmentConflictException(database.getName(), database.getId(), names, ids, env.getName(), env.getId());
        }
    }

    /** 保存数据库资源，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveResource(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, envId);
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(envId);
        }
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        //新增实例到cmdb
        Long ciId = paramObj.getLong("ciId");
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
//        List<String> attrList = new ArrayList<>();
//        List<String> relList = new ArrayList<>();
//        List<String> globalAttrList = new ArrayList<>();
//        JSONArray attrArray = paramObj.getJSONArray("attrList");
//        if (CollectionUtils.isNotEmpty(attrArray)) {
//            attrList = attrArray.toJavaList(String.class);
//        }
//        JSONArray relArray = paramObj.getJSONArray("relList");
//        if (CollectionUtils.isNotEmpty(relArray)) {
//            relList = relArray.toJavaList(String.class);
//        }
//        JSONArray globalAttrArray = paramObj.getJSONArray("globalAttrList");
//        if (CollectionUtils.isNotEmpty(globalAttrArray)) {
//            globalAttrList = globalAttrArray.toJavaList(String.class);
//        }
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        Long id = paramObj.getLong("id");
        if (id != null) {
            //获取实例的具体信息
            CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(id);
            if (instanceCiEntity == null) {
                throw new CiEntityNotFoundException(id);
            }
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), id);
            ciEntityTransactionVo.setCiEntityId(ciEntityVo.getId());
            ciEntityTransactionVo.setCiId(ciEntityVo.getCiId());
            ciEntityTransactionVo.setAllowCommit(true);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
            JSONObject attrEntityData = ciEntityVo.getAttrEntityData();
            if (MapUtils.isNotEmpty(attrEntityData)) {
                ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(attrEntityData.toJSONString()));
            }
            JSONObject relEntityData = ciEntityVo.getRelEntityData();
            if (MapUtils.isNotEmpty(relEntityData)) {
                ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(relEntityData.toJSONString()));
            }
            JSONObject globalAttrEntityData = ciEntityVo.getGlobalAttrEntityData();
            if (MapUtils.isNotEmpty(globalAttrEntityData)) {
                ciEntityTransactionVo.setGlobalAttrEntityData(JSONObject.parseObject(globalAttrEntityData.toJSONString()));
            }
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            //修改属性值
            deployAppConfigService.addAttrEntityDataAndRelEntityData(
                    ciEntityTransactionVo,
                    ciVo.getId(),
                    paramObj,
                    Arrays.asList("name", "ip", "port", "maintenance_window"),
                    Collections.singletonList("APPComponent"),
                    Collections.singletonList("app_environment")
            );
        } else {
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            //添加环境属性、模块关系
            deployAppConfigService.addAttrEntityDataAndRelEntityData(
                    ciEntityTransactionVo,
                    ciVo.getId(),
                    paramObj,
                    Arrays.asList("name", "ip", "port", "maintenance_window"),
                    Collections.singletonList("APPComponent"),
                    Collections.singletonList("app_environment")
            );
        }
        ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
        JSONObject resultObj = new JSONObject();
        Long ciEntityId = ciEntityTransactionVo.getCiEntityId();
        resultObj.put("id", ciEntityId);
        resultObj.put("transactionGroupId", transactionGroupId);
        return resultObj;
    }

    /** 保存环境实例，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveInstance(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, envId);
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(appModuleId);
        }
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        //实例挂环境
        JSONArray instanceIdArray = paramObj.getJSONArray("instanceIdList");
        if (CollectionUtils.isNotEmpty(instanceIdArray)) {
            List<Long> instanceIdList = instanceIdArray.toJavaList(Long.class);
            for (Long instanceId : instanceIdList) {
                //获取实例的具体信息
                CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(instanceId);
                if (instanceCiEntity == null) {
                    throw new CiEntityNotFoundException(instanceId);
                }
                CiEntityVo instanceCiEntityInfo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), instanceId);

                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(instanceCiEntityInfo);
                JSONObject attrEntityData = instanceCiEntityInfo.getAttrEntityData();
                ciEntityTransactionVo.setAttrEntityData(attrEntityData);

                //添加环境属性、模块关系
                deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, instanceCiEntity.getCiId(), paramObj, Collections.singletonList("app_environment"), Collections.singletonList("APPComponent"), Collections.singletonList("app_environment"));

                //设置基础信息
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());

                List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                ciEntityTransactionList.add(ciEntityTransactionVo);
                ciEntityService.saveCiEntity(ciEntityTransactionList);
            }
        } else {
            //新增实例到cmdb
            Long ciId = paramObj.getLong("ciId");
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo paramCiVo = ciCrossoverMapper.getCiById(ciId);
            if (paramCiVo == null) {
                throw new CiNotFoundException(ciId);
            }
            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
            Long id = paramObj.getLong("id");
            if (id != null) {
                //获取实例的具体信息
                CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(id);
                if (instanceCiEntity == null) {
                    throw new CiEntityNotFoundException(id);
                }
                CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), id);
                ciEntityTransactionVo.setCiEntityId(ciEntityVo.getId());
                ciEntityTransactionVo.setCiId(ciEntityVo.getCiId());
                ciEntityTransactionVo.setAllowCommit(true);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
                JSONObject attrEntityData = ciEntityVo.getAttrEntityData();
                if (MapUtils.isNotEmpty(attrEntityData)) {
                    ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(attrEntityData.toJSONString()));
                }
                JSONObject relEntityData = ciEntityVo.getRelEntityData();
                if (MapUtils.isNotEmpty(relEntityData)) {
                    ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(relEntityData.toJSONString()));
                }
                JSONObject globalAttrEntityData = ciEntityVo.getGlobalAttrEntityData();
                if (MapUtils.isNotEmpty(globalAttrEntityData)) {
                    ciEntityTransactionVo.setGlobalAttrEntityData(JSONObject.parseObject(globalAttrEntityData.toJSONString()));
                }
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                //修改属性值
                deployAppConfigService.addAttrEntityDataAndRelEntityData(
                        ciEntityTransactionVo,
                        paramCiVo.getId(),
                        paramObj,
                        Arrays.asList("name", "ip", "port", "maintenance_window"),
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            } else {
                ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                //添加环境属性、模块关系
                deployAppConfigService.addAttrEntityDataAndRelEntityData(
                        ciEntityTransactionVo,
                        paramCiVo.getId(),
                        paramObj,
                        Arrays.asList("name", "ip", "port", "maintenance_window"),
                        Collections.singletonList("APPComponent"),
                        Collections.singletonList("app_environment")
                );
            }

            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            ciEntityTransactionList.add(ciEntityTransactionVo);
            ciEntityService.saveCiEntity(ciEntityTransactionList);
            return ciEntityTransactionVo.getCiEntityId();
        }

        return null;
    }

    /** 保存环境属性，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveAttributes(JSONObject paramObj) {
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        DeployAppConfigEnvAttrVo appEnvAttrVo = JSON.toJavaObject(paramObj, DeployAppConfigEnvAttrVo.class);
        Long nowDate = System.currentTimeMillis();
        appEnvAttrVo.setUpdateTime(nowDate);
        appEnvAttrVo.setLcd(new Date());
        if (CollectionUtils.isNotEmpty(appEnvAttrVo.getKeyValueList())) {
            deployAppConfigMapper.insertAppConfigEnvAttr(appEnvAttrVo);
        }
        deployAppConfigMapper.deleteAppConfigEnvAttr(appEnvAttrVo);
        return null;
    }

    /** 只校验调用方副本，definitions 为 null 表示环境默认变量；不声明事务，失败由调用方返回或回滚。 */
    public void validateVariables(List<DeployAppEnvAutoConfigKeyValueVo> keyValueList,
            List<DeployAppEnvAutoConfigKeyValueVo> definitions) {
        Set<String> keySet = new HashSet<>();
        for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : keyValueList ) {
            if (Objects.equals(keyValueVo.getIsEmpty(), 1)) {
                keyValueVo.setValue(StringUtils.EMPTY);
            } else {
                if (StringUtils.isBlank(keyValueVo.getValue())) {
                    keyValueVo.setValue(null);
                }
            }
            if (keySet.contains(keyValueVo.getKey())) {
                throw new DeployAppConfigEnvAutoConfigKeyRepeatException(keyValueVo.getKey());
            }
            keySet.add(keyValueVo.getKey());
        }
        if (definitions != null) {
            Map<String, DeployAppEnvAutoConfigKeyValueVo> oldKeyValueMap = definitions.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(DeployAppEnvAutoConfigKeyValueVo::getKey, value -> value));
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : keyValueList) {
                DeployAppEnvAutoConfigKeyValueVo oldKeyValueVo = oldKeyValueMap.get(keyValueVo.getKey());
                if (oldKeyValueVo == null) {
                    throw new DeployAppConfigEnvAutoConfigKeyIrregularException(keyValueVo.getKey());
                }
                if (!Objects.equals(oldKeyValueVo.getType(), keyValueVo.getType())) {
                    throw new DeployAppConfigEnvAutoConfigKeyTypeIrregularException(keyValueVo.getKey(), keyValueVo.getType(), oldKeyValueVo.getType());
                }
            }
        }
    }

    /** 保存配置文件适配，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveVariables(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        Long instanceId = paramObj.getLong("instanceId");
        if (instanceId == null) {
            instanceId = 0L;
        }
        List<DeployAppEnvAutoConfigKeyValueVo> keyValueList = new ArrayList<>();
        JSONArray keyValueArray = paramObj.getJSONArray("keyValueList");
        if (CollectionUtils.isNotEmpty(keyValueArray)) {
            keyValueList = keyValueArray.toJavaList(DeployAppEnvAutoConfigKeyValueVo.class);
        }
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        validateVariables(keyValueList, instanceId == 0L ? null : deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(
                new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, 0L)));
        //        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSON.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, instanceId);
        List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(appEnvAutoConfigVo);
        JSONArray tbodyList = getTbodyList(oldKeyValueList, keyValueList);
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            Date nowDate = new Date(System.currentTimeMillis());
            appEnvAutoConfigVo.setLcd(nowDate);
            if (CollectionUtils.isNotEmpty(keyValueList)) {
                appEnvAutoConfigVo.setKeyValueList(keyValueList);
                deployAppConfigMapper.insertAppEnvAutoConfig(appEnvAutoConfigVo);
            }
            deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
            DeployAppEnvAutoConfigAuditVo deployAppEnvAutoConfigAuditVo = new DeployAppEnvAutoConfigAuditVo();
            deployAppEnvAutoConfigAuditVo.setAppSystemId(appSystemId);
            deployAppEnvAutoConfigAuditVo.setAppModuleId(appModuleId);
            deployAppEnvAutoConfigAuditVo.setEnvId(envId);
            deployAppEnvAutoConfigAuditVo.setInstanceId(instanceId);
            deployAppEnvAutoConfigAuditVo.setConfig(TableResultUtil.getResult(tbodyList));
            deployAppConfigMapper.insertAppEnvAutoConfigAudit(deployAppEnvAutoConfigAuditVo);
        }
        Long deleteInstanceId = paramObj.getLong("deleteInstanceId");
        if (deleteInstanceId != null) {
            DeployAppEnvAutoConfigVo deleteAppEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, deleteInstanceId);
            deployAppConfigMapper.deleteAppEnvAutoConfig(deleteAppEnvAutoConfigVo);
        }
        return null;
    }

    /** 生成配置文件适配变更明细。 */
    private JSONArray getTbodyList(List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList, List<DeployAppEnvAutoConfigKeyValueVo> newKeyValueList) {
        oldKeyValueList.sort(Comparator.comparing(DeployAppEnvAutoConfigKeyValueVo::getKey));
        newKeyValueList.sort(Comparator.comparing(DeployAppEnvAutoConfigKeyValueVo::getKey));
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(oldKeyValueList)) {
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : oldKeyValueList) {
                JSONObject tbody = new JSONObject();
                tbody.put("key", keyValueVo.getKey());
                tbody.put("beforeType", keyValueVo.getType());
                tbody.put("beforeValue", keyValueVo.getValue());
                tbody.put("beforeIsEmpty", keyValueVo.getIsEmpty());
                tbody.put("action", "delete");
                tbodyList.add(tbody);
            }
        }
        if (CollectionUtils.isNotEmpty(newKeyValueList)) {
            int lastIndex = -1;
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : newKeyValueList) {
                Integer index = null;
                for (int i = 0; i < tbodyList.size(); i++) {
                    JSONObject tbody = tbodyList.getJSONObject(i);
                    String key = tbody.getString("key");
                    if (Objects.equals(key, keyValueVo.getKey())) {
                        index = i;
                    }
                }
                if (index != null) {
                    JSONObject tbody = tbodyList.getJSONObject(index);
                    tbody.put("afterType", keyValueVo.getType());
                    tbody.put("afterValue", keyValueVo.getValue());
                    tbody.put("afterIsEmpty", keyValueVo.getIsEmpty());
                    tbody.put("action", "update");
                    lastIndex = index;
                } else {
                    lastIndex++;
                    JSONObject tbody = new JSONObject();
                    tbody.put("key", keyValueVo.getKey());
                    tbody.put("afterType", keyValueVo.getType());
                    tbody.put("afterValue", keyValueVo.getValue());
                    tbody.put("afterIsEmpty", keyValueVo.getIsEmpty());
                    tbody.put("action", "insert");
                    tbodyList.add(lastIndex, tbody);
                }
            }
        }
        for (int index = tbodyList.size() - 1; index >= 0; index--) {
            JSONObject tbody = tbodyList.getJSONObject(index);
            String action = tbody.getString("action");
            if (Objects.equals(action, "update")) {
                Integer beforeIsEmpty = tbody.getInteger("beforeIsEmpty");
                Integer afterIsEmpty = tbody.getInteger("afterIsEmpty");
                if (Objects.equals(beforeIsEmpty, afterIsEmpty)) {
                    if (Objects.equals(beforeIsEmpty, 1)) {
                        tbodyList.remove(index);
                    } else {
                        String beforeValue = tbody.getString("beforeValue");
                        String afterValue = tbody.getString("afterValue");
                        if (Objects.equals(beforeValue, afterValue)) {
                            tbodyList.remove(index);
                        }
//                        else {
//                            if (StringUtils.isBlank(beforeValue) && StringUtils.isBlank(afterValue)) {
//                                tbodyList.remove(index);
//                            }
//                        }
                    }
                }
            }
        }
        return tbodyList;
    }
}
