/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/6/22 11:04
 **/
@Service
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
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppEnvAutoConfigVo[].class, desc = "应用配置授权列表")
    })
    @Description(desc = "查询应用环境详细配置信息接口")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONObject envInfo = new JSONObject();
        DeployAppEnvAutoConfigVo envAutoConfigVo = paramObj.toJavaObject(DeployAppEnvAutoConfigVo.class);
        //获取环境 autoConfig

        envAutoConfigVo.setInstanceId(null);
        List<DeployAppEnvAutoConfigKeyValueVo> envAutoConfigList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(envAutoConfigVo);
        envInfo.put("envAutoConfigList", envAutoConfigList);

        //TODO 根据appSystemId获取阶段信息

        //获取实例列表
        List<ResourceVo> instanceList = resourceCenterMapper.getResourceByAppSystemIdAndModuleIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"));
        envInfo.put("instanceList", instanceList);

        JSONObject instanceAutoConfigList = new JSONObject();
        if (CollectionUtils.isNotEmpty(instanceList)) {
            List<Long> instanceIdList = instanceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
            List<DeployAppEnvAutoConfigVo> allInstanceConfigList = deployAppConfigMapper.getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(paramObj.getLong("appSystemId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), instanceIdList);
            if (CollectionUtils.isNotEmpty(allInstanceConfigList)) {
                Map<Long, List<DeployAppEnvAutoConfigKeyValueVo>> allInstanceConfigMap = allInstanceConfigList.stream().collect(Collectors.toMap(DeployAppEnvAutoConfigVo::getInstanceId, DeployAppEnvAutoConfigVo::getKeyValueList));
                for (Map.Entry<Long, List<DeployAppEnvAutoConfigKeyValueVo>> entry : allInstanceConfigMap.entrySet()) {

                }


            }


        }

        //TODO db配置
        return envInfo;
    }
}
