/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/6/22 11:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigEnvInfoApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/env/info/get";
    }

    @Override
    public String getName() {
        return "查询应用环境详细配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppEnvAutoConfigVo[].class, desc = "应用配置环境autoConfig列表")
    })
    @Description(desc = "查询应用环境详细配置信息接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONObject envInfo = new JSONObject();
        DeployAppEnvAutoConfigVo envAutoConfigVo = paramObj.toJavaObject(DeployAppEnvAutoConfigVo.class);

        //获取环境 autoConfig
        List<DeployAppEnvAutoConfigKeyValueVo> envAutoConfigList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(envAutoConfigVo);
        envInfo.put("envAutoConfigList", envAutoConfigList);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        //获取应用实例下的实例列表
        List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(paramObj.toJavaObject(ResourceVo.class), TenantContext.get().getDataDbName());

        //获取实例autoConfig
        if (CollectionUtils.isNotEmpty(instanceIdList)) {

            List<ResourceVo> instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdList(instanceIdList, TenantContext.get().getDataDbName());
            envInfo.put("instanceList", instanceList);
            List<DeployAppEnvAutoConfigVo> instanceAutoConfigList = deployAppConfigMapper.getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), instanceIdList);
            envInfo.put("instanceAutoConfigList", instanceAutoConfigList);

            //补充实例name、ip、port
            Map<Long, ResourceVo> instanceMap = instanceList.stream().collect(Collectors.toMap(ResourceVo::getId, e -> e));
            for (DeployAppEnvAutoConfigVo autoConfigVo : instanceAutoConfigList) {
                ResourceVo instanceResourceVo = instanceMap.get(autoConfigVo.getInstanceId());
                if (instanceResourceVo == null) {
                    continue;
                }
                autoConfigVo.setInstanceName(instanceResourceVo.getName());
                autoConfigVo.setInstanceIp(instanceResourceVo.getIp());
                autoConfigVo.setInstancePort(instanceResourceVo.getPort());
            }
        }

        /*获取DB配置，核实dbResourceId是否存在，不存在则删除关系*/
        List<DeployAppConfigEnvDBConfigVo> appConfigEnvDBConfigList = deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"));
        if (CollectionUtils.isEmpty(appConfigEnvDBConfigList)) {
            return envInfo;
        }
        Map<Long, DeployAppConfigEnvDBConfigVo> dbConfigMap = appConfigEnvDBConfigList.stream().collect(Collectors.toMap(DeployAppConfigEnvDBConfigVo::getDbResourceId, e->e));
        envInfo.put("DBConfigList", dbConfigMap.values());

        //1、根据resourceId 查询现在仍存在的 配置项
        List<Long> deleteDbIdList = appConfigEnvDBConfigList.stream().map(DeployAppConfigEnvDBConfigVo::getDbResourceId).collect(Collectors.toList());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        List<CiEntityVo> nowCiEntityVoList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(deleteDbIdList);
        List<Long> exitDbIdList = nowCiEntityVoList.stream().map(CiEntityVo::getId).collect(Collectors.toList());

        //2、取差集
        deleteDbIdList.removeAll(exitDbIdList);

        //3、删除发布残留的DBResourceId
        if (CollectionUtils.isNotEmpty(deleteDbIdList)) {
            List<Long> needDeleteDbConfigIdList = new ArrayList<>();
            for (Long oldDbId : deleteDbIdList) {
                needDeleteDbConfigIdList.add(dbConfigMap.get(oldDbId).getId());
                dbConfigMap.remove(oldDbId);
            }
            if (CollectionUtils.isNotEmpty(needDeleteDbConfigIdList)) {
                deployAppConfigMapper.deleteAppConfigDBConfigByIdList(needDeleteDbConfigIdList);
                deployAppConfigMapper.deleteAppConfigDBConfigAccountByDBConfigIdList(needDeleteDbConfigIdList);
            }
        }
        return envInfo;
    }
}
