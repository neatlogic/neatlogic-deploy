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
public class ListDeployAppInstanceCiAttrApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;


    @Override
    public String getName() {
        return "获取发布应用实例模型的属性列表";
    }

    @Override
    public String getToken() {
            return "deploy/app/instance/ci/attr/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "是否返回全部属性"),
            @Param(name = "attrNameList", type = ApiParamType.JSONARRAY, desc = "需要返回的属性列表")
    })
    @Output({@Param(type = ApiParamType.JSONOBJECT, desc = "发布应用模块实例的属性列表")})
    @Description(desc = "获取发布应用模块实例的属性列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = null;
        Long ciId = paramObj.getLong("ciId");
        if (ciId != null) {
            appCiVo = ciCrossoverMapper.getCiById(ciId);
            if (appCiVo == null) {
                throw new CiNotFoundException(ciId);
            }
        } else {
            appCiVo = ciCrossoverMapper.getCiByName("AppIns");
            if (appCiVo == null) {
                throw new CiNotFoundException("AppIns");
            }
        }
        return deployAppConfigService.getDeployCiAttrList(appCiVo.getId(), paramObj.getInteger("isAll"), paramObj.getJSONArray("attrNameList"));
    }
}
