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

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployInstanceVersionMapper;
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

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

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
        List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(paramObj.toJavaObject(ResourceVo.class));

        //获取实例autoConfig
        if (CollectionUtils.isNotEmpty(instanceIdList)) {

            List<ResourceVo> instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdList(instanceIdList);
            // 补充实例当前版本信息
            List<DeployInstanceVersionVo> instanceVersionVoList = deployInstanceVersionMapper.getDeployInstanceVersionByEnvIdAndInstanceIdList(envAutoConfigVo.getAppSystemId(), envAutoConfigVo.getAppModuleId(), envAutoConfigVo.getEnvId(), instanceIdList);
            if (CollectionUtils.isNotEmpty(instanceVersionVoList)) {
                Map<Long, DeployInstanceVersionVo> versionMap = instanceVersionVoList.stream().collect(Collectors.toMap(DeployInstanceVersionVo::getResourceId, e -> e));
                JSONArray instanceArray = new JSONArray();
                for (ResourceVo resourceVo : instanceList) {
                    JSONObject instanceObj = (JSONObject) JSONObject.toJSON(resourceVo);
                    instanceObj.put("version", versionMap.containsKey(resourceVo.getId()) ? versionMap.get(resourceVo.getId()).getVersion() : "");
                    instanceObj.put("instanceVersion", versionMap.get(resourceVo.getId()));
                    instanceArray.add(instanceObj);
                }
                envInfo.put("instanceList", instanceArray);
            } else {
                envInfo.put("instanceList", instanceList);
            }
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
        Map<String, DeployAppConfigEnvDBConfigVo> dbSchemaConfigVoMap = appConfigEnvDBConfigList.stream().collect(Collectors.toMap(DeployAppConfigEnvDBConfigVo::getDbSchema, e -> e));
        envInfo.put("DBConfigList", dbSchemaConfigVoMap.values());

        //1、根据resourceId 查询现在仍存在的 配置项
        List<Long> deleteDbIdList = appConfigEnvDBConfigList.stream().map(DeployAppConfigEnvDBConfigVo::getDbResourceId).collect(Collectors.toList());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        List<CiEntityVo> nowCiEntityVoList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(deleteDbIdList);
        List<Long> exitDbIdList = nowCiEntityVoList.stream().map(CiEntityVo::getId).collect(Collectors.toList());

        //2、取差集
        deleteDbIdList.removeAll(exitDbIdList);

        //3、删除发布残留的DBResourceId
        if (CollectionUtils.isNotEmpty(deleteDbIdList)) {
            List<String> needDeleteDbSchemaList = new ArrayList<>();
            List<Long> needDeleteDbConfigIdList = new ArrayList<>();
            //删除不存在的db的配置
            for (DeployAppConfigEnvDBConfigVo dbConfigVo : dbSchemaConfigVoMap.values()) {
                if (dbConfigVo.getDbResourceId() != null && deleteDbIdList.contains(dbConfigVo.getDbResourceId())) {
                    needDeleteDbConfigIdList.add(dbConfigVo.getId());
                    needDeleteDbSchemaList.add(dbConfigVo.getDbSchema());
                }
            }
            if (CollectionUtils.isNotEmpty(needDeleteDbConfigIdList)) {
                deployAppConfigMapper.deleteAppConfigDBConfigByIdList(needDeleteDbConfigIdList);
            }
            if (CollectionUtils.isNotEmpty(needDeleteDbSchemaList)) {
                for (String schema : needDeleteDbSchemaList) {
                    dbSchemaConfigVoMap.remove(schema);
                }
            }
        }
        return envInfo;
    }
}
