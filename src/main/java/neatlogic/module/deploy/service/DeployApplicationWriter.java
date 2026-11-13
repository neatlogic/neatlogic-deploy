/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.crossover.IGlobalAttrCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrItemIsNotExistsException;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployAppConfigActionType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityActionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import neatlogic.framework.dto.AuthorityVo;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dependency.handler.NotifyPolicyDeployJobDependencyHandler;
import neatlogic.module.deploy.notify.handler.DeployJobNotifyPolicyHandler;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在领域模块内维护共享校验；仅写入方法声明事务，预检异常由调用方决定返回或回滚。 */
@Service
public class DeployApplicationWriter {
    @Resource private DeployAppConfigMapper deployAppConfigMapper;
    @Resource private DeployAppConfigService deployAppConfigService;
    @Resource private DeployAppAuthorityService deployAppAuthorityService;

    /** 保存应用系统及初始授权，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveSystem(JSONObject paramObj, boolean grantCreator) throws Exception {

        Long appSystemId = paramObj.getLong("id");

        //校验编辑配置的操作权限
        if (appSystemId != null) {
            deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        }

        List<Long> stateIdList = new ArrayList<>();
        List<Long> ownerIdList = new ArrayList<>();
        //构建数据结构
        JSONArray stateIdArray = paramObj.getJSONArray("state");
        if (CollectionUtils.isNotEmpty(stateIdArray)) {
            stateIdList = stateIdArray.toJavaList(Long.class);
        }
        JSONArray ownerIdArray = paramObj.getJSONArray("owner");
        if (CollectionUtils.isNotEmpty(ownerIdArray)) {
            ownerIdList = ownerIdArray.toJavaList(Long.class);
        }
        paramObj.put("stateIdList", stateIdList);
        paramObj.put("ownerIdList", ownerIdList);

        //定义需要插入的字段
        List<String> needUpdateAttrList = Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description");
        //获取应用系统的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APP");

        //保存
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityTransactionVo ciEntityTransactionVo = null;
        if (appSystemId == null) {

            /*新增应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            ciEntityTransactionVo = new CiEntityTransactionVo();
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, appCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>(), new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {

            /*编辑应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            CiEntityVo systemCiEntityInfo = ciEntityService.getCiEntityById(appCiVo.getId(), appSystemId);
            if (systemCiEntityInfo == null) {
                throw new CiEntityNotFoundException(appSystemId);
            }

            ciEntityTransactionVo = new CiEntityTransactionVo(systemCiEntityInfo);
            ciEntityTransactionVo.setAttrEntityData(systemCiEntityInfo.getAttrEntityData());
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, appCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>(), new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        }
        //3、保存系统（配置项）
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);

        // 添加应用时，给创建用户添加全部的操作权限，包括查看作业/配置、编辑配置、版本&制品管理、超级流水线权限
        if (grantCreator && appSystemId == null) {
            DeployAppConfigAuthorityVo deployAppConfigAuthorityVo = new DeployAppConfigAuthorityVo();
            deployAppConfigAuthorityVo.setAppSystemId(ciEntityTransactionVo.getCiEntityId());
            deployAppConfigAuthorityVo.setAuthType(GroupSearch.USER.getValue());
            deployAppConfigAuthorityVo.setAuthUuid(UserContext.get().getUserUuid());
            deployAppConfigAuthorityVo.setLcd(new Date());
            deployAppConfigAuthorityVo.setLcu(UserContext.get().getUserUuid());
            List<DeployAppConfigAuthorityActionVo> actionList = new ArrayList<>();
            for (DeployAppConfigAction deployAppConfigAction : DeployAppConfigAction.values()) {
                actionList.add(new DeployAppConfigAuthorityActionVo(deployAppConfigAction.getValue(), DeployAppConfigActionType.OPERATION.getValue()));
            }
            deployAppConfigAuthorityVo.setActionList(actionList);
            deployAppConfigMapper.insertAppConfigAuthority(deployAppConfigAuthorityVo);
        }
        return ciEntityTransactionVo.getCiEntityId();
    }

    /** 保存模块关联环境，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object associateEnvironments(JSONObject paramObj) throws Exception {
        JSONArray envIdArray = paramObj.getJSONArray("envIdList");
        List<Long> envIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(envIdArray)) {
            envIdList = envIdArray.toJavaList(Long.class);
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId) == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(appModuleId);
        }

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);

        IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
        List<GlobalAttrItemVo> globalAttrItemList = new ArrayList<>();
        GlobalAttrVo globalAttrVo = new GlobalAttrVo();
        globalAttrVo.setIsActive(1);
        globalAttrVo.setName("app_environment");
        List<GlobalAttrVo> globalAttrList = globalAttrCrossoverMapper.searchGlobalAttr(globalAttrVo);
        if (CollectionUtils.isNotEmpty(globalAttrList)) {
            globalAttrVo = globalAttrList.get(0);
            globalAttrItemList = globalAttrCrossoverMapper.getAllGlobalAttrItemByAttrId(globalAttrVo.getId());
        }
        List<Long> globalAttrItemIdList = globalAttrItemList.stream().map(GlobalAttrItemVo::getId).collect(Collectors.toList());
        List<Long> notExistIdList = ListUtils.removeAll(envIdList, globalAttrItemIdList);
        if (CollectionUtils.isNotEmpty(notExistIdList)) {
            List<String> notExistIdStrList = new ArrayList<>();
            for (Long id : notExistIdList) {
                notExistIdStrList.add(id.toString());
            }
            throw new GlobalAttrItemIsNotExistsException(globalAttrVo, String.join(",", notExistIdStrList));
        }

        deployAppConfigMapper.insertAppConfigEnv(appSystemId, appModuleId, envIdList);
        return null;
    }

    /** 保存应用授权，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveAuthority(JSONObject paramObj) {

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.AUTH);

        DeployAppConfigAuthorityVo deployAppConfigAuthorityVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);
        Date nowTime = new Date(System.currentTimeMillis());
        List<String> authUuidList = new ArrayList<>();
        deployAppConfigAuthorityVo.setLcd(nowTime);
        for (AuthorityVo authorityVo : deployAppConfigAuthorityVo.getAuthorityList()) {
            authUuidList.add(authorityVo.getUuid());
            deployAppConfigAuthorityVo.setAuthUuid(authorityVo.getUuid());
            deployAppConfigAuthorityVo.setAuthType(authorityVo.getType());
            deployAppConfigMapper.insertAppConfigAuthority(deployAppConfigAuthorityVo);
        }

        //如果是编辑，则需要删除多余权限
        if (deployAppConfigAuthorityVo.getIsEdit() == 1) {
            deployAppConfigMapper.deleteAppConfigAuthorityByAppIdAndAuthUuidListAndLcd(deployAppConfigAuthorityVo.getAppSystemId(), authUuidList, nowTime);
        }
        return null;
    }

    /** 通知规范化不声明事务，预检可返回字段错误，正式保存仍由写入事务保证原子性。 */
    public InvokeNotifyPolicyConfigVo validateNotification(JSONObject values) {
        InvokeNotifyPolicyConfigVo config = values.toJavaObject(InvokeNotifyPolicyConfigVo.class);
        config.setHandler(DeployJobNotifyPolicyHandler.class.getName());
        return CrossoverServiceFactory.<INotifyServiceCrossoverService>getApi(INotifyServiceCrossoverService.class)
                .regulateNotifyPolicyConfig(config);
    }

    /** 保存通知配置，调用方与原页面共享权限校验和写入规则。 */
    @Transactional(rollbackFor = Exception.class)
    public Object saveNotification(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        DependencyManager.delete(NotifyPolicyDeployJobDependencyHandler.class, appSystemId);
        Integer isCustom = paramObj.getInteger("isCustom");
        Long policyId = paramObj.getLong("policyId");
        if (Objects.equals(isCustom, 1)) {
            DependencyManager.insert(NotifyPolicyDeployJobDependencyHandler.class, policyId, appSystemId);
        } else {
            policyId = -1L;
        }
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = validateNotification(paramObj);
        String configStr = JSONObject.toJSONString(invokeNotifyPolicyConfigVo);
        deployAppConfigMapper.saveDeloyJobNotifyPolicy(appSystemId, policyId, configStr);
        return null;
    }
}
