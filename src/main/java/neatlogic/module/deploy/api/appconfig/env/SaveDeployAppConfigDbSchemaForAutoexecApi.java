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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigDBSchemaActionIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/8/22 16:38
 */
@Service
@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployAppConfigDbSchemaForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigdbschemaforautoexecapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/schemas/save/forautoexec";
    }

    @Input({
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.runnerid"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.runnergroup"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.jobid"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.phasename"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.sysid"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.moduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.envid"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.sysname"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.modulename"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.envname"),
            @Param(name = "dbSchemas", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.input.param.desc.dbschemas"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.savedeployappconfigdbschemaforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray dbSchemaArray = paramObj.getJSONArray("dbSchemas");
        if (CollectionUtils.isEmpty(dbSchemaArray)) {
            return null;
        }
        List<String> dbSchemaList = dbSchemaArray.toJavaList(String.class);
        List<String> dbSchemaIrregularList = new ArrayList<>();
        List<DeployAppConfigEnvDBConfigVo> insertDbConfigVoList = new ArrayList<>();
        for (String dbSchema : dbSchemaList) {
            if (!RegexUtils.isMatch(dbSchema, RegexUtils.DB_SCHEMA)) {
                dbSchemaIrregularList.add(dbSchema);
            } else {
                insertDbConfigVoList.add(new DeployAppConfigEnvDBConfigVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), dbSchema));
            }
        }
        if (CollectionUtils.isNotEmpty(dbSchemaIrregularList)) {
            throw new DeployAppConfigDBSchemaActionIrregularException(dbSchemaIrregularList);
        }

        deployAppConfigMapper.insertBatchAppConfigEnvDBConfig(insertDbConfigVoList);
        return null;
    }
}
