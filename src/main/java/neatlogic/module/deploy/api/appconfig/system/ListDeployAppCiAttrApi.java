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
public class ListDeployAppCiAttrApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Override
    public String getName() {
        return "nmdaas.listdeployappciattrapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/ci/attr/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "nmdaas.listdeployappciattrapi.input.param.desc.isall"),
            @Param(name = "attrNameList", type = ApiParamType.JSONARRAY, desc = "nmdaas.listdeployappciattrapi.input.param.desc.attrnamelist")
    })
    @Output({@Param(type = ApiParamType.JSONOBJECT, desc = "nmdaas.listdeployappciattrapi.output.param.desc.return")})
    @Description(desc = "nmdaas.listdeployappciattrapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APP");
        if (appCiVo == null) {
            throw new CiNotFoundException("APP");
        }
        return deployAppConfigService.getDeployCiAttrList(appCiVo.getId(), paramObj.getInteger("isAll"), paramObj.getJSONArray("attrNameList"));
    }
}
