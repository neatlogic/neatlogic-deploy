/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployAppConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/11/7 11:10
 */

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppModuleCiAttrApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;


    @Override
    public String getName() {
        return "获取发布应用模块模型的属性列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/module/ci/attr/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "是否返回全部属性"),
            @Param(name = "attrNameList", type = ApiParamType.JSONARRAY, desc = "需要返回的属性列表")
    })
    @Output({@Param(type = ApiParamType.JSONOBJECT, desc = "发布应用模块模型的属性列表")})
    @Description(desc = "获取发布应用模块模型的属性列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APPComponent");
        if (appCiVo == null) {
            throw new CiNotFoundException("APPComponent");
        }
        return deployAppConfigService.getDeployCiAttrList(appCiVo.getId(), paramObj.getInteger("isAll"), paramObj.getJSONArray("attrNameList"));
    }
}
