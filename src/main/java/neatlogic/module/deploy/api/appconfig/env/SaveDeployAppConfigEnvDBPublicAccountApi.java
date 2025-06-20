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
import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/22 15:55
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigEnvDBPublicAccountApi extends PrivateApiComponentBase {

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/publicaccount/save";
    }

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvdbpublicaccountapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.resourceid"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.accountidlist")
    })
    @Description(desc = "nmdaae.savedeployappconfigenvdbpublicaccountapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long envId = paramObj.getLong("envId");
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, envId);
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        Long resourceId = paramObj.getLong("resourceId");
        if (deployAppConfigMapper.getDatabaseById(resourceId) == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        List<Long> accountIdList = new ArrayList<>();
        JSONArray accountIdArray = paramObj.getJSONArray("accountIdList");
        if (CollectionUtils.isNotEmpty(accountIdArray)) {
            accountIdList = accountIdArray.toJavaList(Long.class);
        }
        IResourceCenterAccountCrossoverService resourceCenterAccountCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
        return resourceCenterAccountCrossoverService.saveResourceAccount(resourceId, accountIdList);
    }
}
