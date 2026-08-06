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
package neatlogic.module.deploy.api.type;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/12/8 14:55
 */

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployTypeStatusApi extends PrivateApiComponentBase {

    @Resource
    DeployTypeMapper deployTypeMapper;

    @Resource
    AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "nmdat.updatedeploytypestatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/type/status/update";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmdat.updatedeploytypestatusapi.input.param.desc.id"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "nmdat.updatedeploytypestatusapi.input.param.desc.isactive"),
    })
    @Output({
    })
    @Description(desc = "nmdat.updatedeploytypestatusapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (autoexecTypeMapper.checkTypeIsExistsById(id) == 0) {
            throw new AutoexecTypeNotFoundException(id);
        }
        deployTypeMapper.insertTypeActive(paramObj.getInteger("isActive"), id);
        return null;
    }
}
