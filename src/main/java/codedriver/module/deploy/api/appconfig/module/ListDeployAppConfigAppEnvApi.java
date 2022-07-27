/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @since 2022/6/17 11:00
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/env/list";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "isHasEnv", type = ApiParamType.INTEGER, desc = "是否拥有环境 (0:查找现没有的环境，1：查找现有的环境)"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "发布应用配置的应用系统环境列表"),
    })
    @Description(desc = "查询发布应用配置的应用系统环境列表(用于应用树的环境下拉、发布作业时通过模块列表查询环境列表)")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        List<DeployAppEnvironmentVo> returnEnvList = null;
        if (Objects.nonNull(paramObj.getInteger("isHasEnv")) && paramObj.getInteger("isHasEnv") == 0) {
            returnEnvList = deployAppConfigMapper.getDeployAppHasNotEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleId, TenantContext.get().getDataDbName());
        } else {
            returnEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, Collections.singletonList(appModuleId), TenantContext.get().getDataDbName());
            if (CollectionUtils.isEmpty(returnEnvList)) {
                return new ArrayList<>();
            }
            /*如果发现发布存的环境在cmdb已经有挂实例了，则删除发布多余的环境*/
            List<Long> envIdList = returnEnvList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());

            //查询发布多余的环境idList
            List<Long> sameEnvIdList = HashMultiset.create(envIdList).entrySet().stream().filter(w -> w.getCount() > 1).map(Multiset.Entry::getElement).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(sameEnvIdList)) {
                return returnEnvList;
            }

            //删除发布多余的环境idList
            deployAppConfigMapper.deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvIdList(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), sameEnvIdList);
            Map<Long, DeployAppEnvironmentVo> returnEnvMap = returnEnvList.stream().collect(Collectors.toMap(e -> e.getId() + e.getIsDeletable(), e -> e));
            for (Long envId : sameEnvIdList) {
                returnEnvMap.remove(envId + 1);
            }
            return returnEnvMap.values();
        }
        return returnEnvList;
    }
}
