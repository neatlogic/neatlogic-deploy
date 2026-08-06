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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@Deprecated
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateDeployJobApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(CreateDeployJobApi.class);
    @Resource
    DeployVersionMapper deployVersionMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    private DeployJobService deployJobService;

    @Override
    public String getName() {
        return "nmdaj.createdeployjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmdaj.createdeployjobapi.input.param.desc.scenarioname"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid"),
            @Param(name = "appSystemAbbrName", type = ApiParamType.STRING, desc = "term.cmdb.appsystemabbrname"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "appModuleAbbrName", type = ApiParamType.STRING, desc = "term.cmdb.appmoduleabbrname"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "term.cmdb.envname"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "nmdaj.createdeployjobapi.input.param.desc.source"),
            @Param(name = "isNeedFire", type = ApiParamType.INTEGER, desc = "nmdaj.createdeployjobapi.input.param.desc.isneedfire"),

    })
    @Description(desc = "nmdaj.createdeployjobapi.getname")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Integer isNeedFire = jsonObj.getInteger("isNeedFire");
        JSONObject result = new JSONObject();
        DeployJobVo jobVo = JSON.toJavaObject(jsonObj, DeployJobVo.class);
        if (isNeedFire == null || isNeedFire == 1) {
            deployJobService.createJobAndFire(jobVo);
        } else {
            deployJobService.createJob(jobVo);
        }
        result.put("id", jobVo.getId());
        result.put("name", jobVo.getName());
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create";
    }

}
