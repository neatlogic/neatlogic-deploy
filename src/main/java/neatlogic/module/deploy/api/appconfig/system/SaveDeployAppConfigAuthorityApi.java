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

package neatlogic.module.deploy.api.appconfig.system;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import neatlogic.framework.dto.AuthorityVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAuthorityApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/authority/save";
    }

    @Override
    public String getName() {
        return "保存应用配置权限";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "授权列表"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "授权操作列表"),
            @Param(name = "isEdit", type = ApiParamType.INTEGER, isRequired = true, desc = "是否编辑，0：否，1：是"),
    })
    @Output({
    })
    @Description(desc = "保存应用配置权限")
    @Override
    public Object myDoService(JSONObject paramObj) {

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

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
}
