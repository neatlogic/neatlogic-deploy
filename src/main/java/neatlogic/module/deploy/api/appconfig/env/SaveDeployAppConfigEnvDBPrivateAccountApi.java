/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNameRepeatsException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigEnvDBPrivateAccountApi extends PrivateApiComponentBase {

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvdbprivateaccountapi.getname";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.resourceid"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 200, isRequired = true, desc = "common.name"),
            @Param(name = "account", type = ApiParamType.STRING, maxLength = 80, desc = "common.username"),
            @Param(name = "passwordPlain", type = ApiParamType.STRING, isRequired = false, desc = "common.password"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.protocol"),
            @Param(name = "port", type = ApiParamType.INTEGER, isRequired = false, desc = "term.cmdb.port"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "common.tagidlist"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "common.type"),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "common.isdefault"),
    })
    @Description(desc = "nmdaae.savedeployappconfigenvdbprivateaccountapi.getname")
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
        IResourceCenterAccountCrossoverService resourceCenterAccountCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
        AccountVo paramAccountVo = JSON.toJavaObject(paramObj, AccountVo.class);
        Long id = paramObj.getLong("id");
        return resourceCenterAccountCrossoverService.saveAccount(id, paramAccountVo);
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/privateaccount/save";
    }

    public IValid name() {
        return value -> {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountVo vo = JSON.toJavaObject(value, AccountVo.class);
            if (Objects.equals(AccountType.PUBLIC.getValue(), vo.getType())) {
                if (resourceAccountCrossoverMapper.checkAccountNameIsRepeats(vo) > 0) {
                    return new FieldValidResultVo(new ResourceCenterAccountNameRepeatsException(vo.getName()));
                }
            } else {
                Long resourceId = vo.getResourceId();
                if (resourceId == null) {
                    throw new ParamNotExistsException("资产ID（resourceId）");
                }
                List<AccountVo> accountVoList = resourceAccountCrossoverMapper.getResourceAccountListByResourceId(resourceId);
                for (AccountVo accountVo : accountVoList) {
                    if (Objects.equals(vo.getName(), accountVo.getName()) && !Objects.equals(vo.getId(), accountVo.getId())) {
                        return new FieldValidResultVo(new ResourceCenterAccountNameRepeatsException(vo.getName()));
                    }
                }
            }
            return new FieldValidResultVo();
        };
    }
}
