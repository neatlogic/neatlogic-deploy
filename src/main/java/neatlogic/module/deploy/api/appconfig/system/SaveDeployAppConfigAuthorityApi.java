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

package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONObject;
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
