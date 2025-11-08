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
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/8/22 16:38
 */
@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployAppConfigAutoConfigForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存某个环境的AutoConfig配置的key";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/autoCfgKeys/save/forautoexec";
    }

    @Input({
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "Runner的ID"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "runner组信息"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用名"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境名"),
            @Param(name = "autoCfgKeys", type = ApiParamType.JSONARRAY, desc = "key列表"),
    })
    @Output({
    })
    @Description(desc = "发布作业专用-保存某个环境的AutoConfig配置的key")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray autoCfgKeyArray = paramObj.getJSONArray("autoCfgKeys");
        if (CollectionUtils.isEmpty(autoCfgKeyArray)) {
            return null;
        }
        List<String> autoCfgKeyList = autoCfgKeyArray.toJavaList(String.class);
        List<DeployAppEnvAutoConfigKeyValueVo> insertAutoConfigKeyValueVoList = new ArrayList<>();
        for (String autoCfgKey : autoCfgKeyList) {
            insertAutoConfigKeyValueVoList.add(new DeployAppEnvAutoConfigKeyValueVo(autoCfgKey));
        }
        deployAppConfigMapper.insertAppEnvAutoConfigNew(new DeployAppEnvAutoConfigVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), insertAutoConfigKeyValueVoList));
        return null;
    }
}
