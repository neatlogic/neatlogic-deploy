/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.app.env;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
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

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/6/22 11:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigEnvInfoApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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

        //获取应用实例下的实例列表
        List<Long> instanceIdList = resourceCenterMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(paramObj.toJavaObject(ResourceVo.class), TenantContext.get().getDataDbName());

        //获取实例autoConfig
        if (CollectionUtils.isNotEmpty(instanceIdList)) {

            List<ResourceVo> instanceList = resourceCenterMapper.getAppInstanceResourceListByIdList(instanceIdList, TenantContext.get().getDataDbName());
            envInfo.put("instanceList", instanceList);
            List<DeployAppEnvAutoConfigVo> instanceAutoConfigList = deployAppConfigMapper.getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), instanceIdList);
            envInfo.put("instanceAutoConfigList", instanceAutoConfigList);
        }
        //DB配置
        List<DeployAppConfigEnvDBConfigVo> appConfigEnvDBConfigList = deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"));
        envInfo.put("DBConfigList", appConfigEnvDBConfigList);

        return envInfo;
    }
}
