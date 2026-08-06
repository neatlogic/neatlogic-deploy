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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineParamApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/param/list";
    }

    @Override
    public String getName() {
        return "nmdaa.listdeployapppipelineparamapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaa.listdeployapppipelineparamapi.input.param.desc.appsystemid")
    })
    @Output({
            @Param(explode = AutoexecParamVo[].class, desc = "nmdaa.listdeployapppipelineparamapi.output.param.desc.return")
    })
    @Description(desc = "nmdaa.listdeployapppipelineparamapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long appSystemId = jsonObj.getLong("appSystemId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            return new ArrayList<>();
        }
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return new ArrayList<>();
        }
        List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
        if (CollectionUtils.isEmpty(runtimeParamList)) {
            return new ArrayList<>();
        }
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
            autoexecServiceCrossoverService.mergeConfig(autoexecParamVo);
        }
        return runtimeParamList;
    }
}
