/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/save";
    }

    @Override
    public String getName() {
        return "保存应用环境实例autoConfig";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
            @Param(name = "deleteInstanceId", type = ApiParamType.LONG, desc = "删除的应用实例 id"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "应用实例 id"),
            @Param(name = "keyValueList", type = ApiParamType.JSONARRAY, desc = "[{\"id\": xxx,\"key\": xxx,\"value\":xxx}]"),
    })
    @Output({
    })
    @Description(desc = "保存应用环境实例autoConfig接口")
    @Override
    public Object myDoService(JSONObject paramObj) {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSONObject.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        Date nowDate = new Date(System.currentTimeMillis());
        appEnvAutoConfigVo.setLcd(nowDate);
        if (CollectionUtils.isNotEmpty(appEnvAutoConfigVo.getKeyValueList())) {
            deployAppConfigMapper.insertAppEnvAutoConfig(appEnvAutoConfigVo);
        }
        deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
        Long deleteInstanceId = paramObj.getLong("deleteInstanceId");
        if (deleteInstanceId != null) {
            DeployAppEnvAutoConfigVo deleteAppEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), deleteInstanceId);
            deployAppConfigMapper.deleteAppEnvAutoConfig(deleteAppEnvAutoConfigVo);
        }
        return null;
    }
}
