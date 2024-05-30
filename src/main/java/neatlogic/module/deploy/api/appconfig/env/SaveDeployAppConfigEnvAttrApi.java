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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvAttrVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author lvzk
 * @since 2024/5/29 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveDeployAppConfigEnvAttrApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/attr/save";
    }

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvattrapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmraa.getappapi.input.param.desc"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.moduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "keyValueList", type = ApiParamType.JSONARRAY, desc = "[{\"id\": xxx,\"key\": xxx,\"value\":xxx}]"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.savedeployappconfigenvattrapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
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
}
