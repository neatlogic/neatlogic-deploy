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
package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployApplicationWriter;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.APP_CONFIG_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/12/29 17:11
 */
@Service
@Transactional
@AuthAction(action = APP_CONFIG_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployJobNotifyPolicyApi extends PrivateApiComponentBase {

    @Resource
    private DeployApplicationWriter writer;

    @Override
    public String getName() {
        return "nmdaj.savedeployjobnotifypolicyapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "policyId", type = ApiParamType.LONG, desc = "term.framework.policyid"),
            @Param(name = "isCustom", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "nmdaj.savedeployjobnotifypolicyapi.input.param.desc.iscustom"),
            @Param(name = "paramMappingList", type = ApiParamType.JSONARRAY, desc = "nmdaj.savedeployjobnotifypolicyapi.input.param.desc.parammappinglist"),
            @Param(name = "excludeTriggerList", type = ApiParamType.JSONARRAY, desc = "nmdaj.savedeployjobnotifypolicyapi.input.param.desc.excludetriggerlist")
    })
    @Output({
    })
    @Description(desc = "nmdaj.savedeployjobnotifypolicyapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return writer.saveNotification(paramObj);
    }

    @Override
    public String getToken() {
        return "deploy/job/notify/policy/save";
    }
}
