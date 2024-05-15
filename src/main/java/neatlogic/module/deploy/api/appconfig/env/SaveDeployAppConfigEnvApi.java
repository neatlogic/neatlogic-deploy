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

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IGlobalAttrCrossoverMapper;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrItemIsNotExistsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "term.cmdb.envidlist"),
    })
    @Output({})
    @Description(desc = "nmdaae.savedeployappconfigenvapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray envIdArray = paramObj.getJSONArray("envIdList");
        List<Long> envIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(envIdArray)) {
            envIdList = envIdArray.toJavaList(Long.class);
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId) == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(appModuleId);
        }

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);

        IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
        List<GlobalAttrItemVo> globalAttrItemList = new ArrayList<>();
        GlobalAttrVo globalAttrVo = new GlobalAttrVo();
        globalAttrVo.setIsActive(1);
        globalAttrVo.setName("app_environment");
        List<GlobalAttrVo> globalAttrList = globalAttrCrossoverMapper.searchGlobalAttr(globalAttrVo);
        if (CollectionUtils.isNotEmpty(globalAttrList)) {
            globalAttrVo = globalAttrList.get(0);
            globalAttrItemList = globalAttrVo.getItemList();
        }
        List<Long> globalAttrItemIdList = globalAttrItemList.stream().map(GlobalAttrItemVo::getId).collect(Collectors.toList());
        List<Long> notExistIdList = ListUtils.removeAll(envIdList, globalAttrItemIdList);
        if (CollectionUtils.isNotEmpty(notExistIdList)) {
            List<String> notExistIdStrList = new ArrayList<>();
            for (Long id : notExistIdList) {
                notExistIdStrList.add(id.toString());
            }
            throw new GlobalAttrItemIsNotExistsException(globalAttrVo, String.join(",", notExistIdStrList));
        }

        deployAppConfigMapper.insertAppConfigEnv(appSystemId, appModuleId, envIdList);
        return null;
    }
}
