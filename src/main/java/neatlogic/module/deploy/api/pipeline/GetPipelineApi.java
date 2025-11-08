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

package neatlogic.module.deploy.api.pipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineGroupVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineLaneVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.exception.pipeline.DeployPipelineNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdap.getpipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/get";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "common.id", isRequired = true)
    })
    @Output({@Param(explode = PipelineVo.class)})
    @Description(desc = "nmdap.getpipelineapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(id);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundEditTargetException(id);
        }
        List<DeployAppConfigVo> appConfigList = new ArrayList<>();
        List<Long> appSystemIdList = new ArrayList<>();
        List<PipelineLaneVo> laneList = pipelineVo.getLaneList();
        if (CollectionUtils.isNotEmpty(laneList)) {
            for (PipelineLaneVo pipelineLaneVo : laneList) {
                List<PipelineGroupVo> groupList = pipelineLaneVo.getGroupList();
                if (CollectionUtils.isNotEmpty(groupList)) {
                    for (PipelineGroupVo pipelineGroupVo : groupList) {
                        List<PipelineJobTemplateVo> jobTemplateList = pipelineGroupVo.getJobTemplateList();
                        if (CollectionUtils.isNotEmpty(jobTemplateList)) {
                            for (PipelineJobTemplateVo pipelineJobTemplateVo : jobTemplateList) {
                                Long appSystemId = pipelineJobTemplateVo.getAppSystemId();
                                if (appSystemId != null && !appSystemIdList.contains(appSystemId)) {
                                    appSystemIdList.add(appSystemId);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            appConfigList = deployAppConfigMapper.getAppConfigListByAppSystemIdList(appSystemIdList);
            if (CollectionUtils.isNotEmpty(appConfigList)) {
                for (DeployAppConfigVo deployAppConfigVo : appConfigList) {
                    DeployPipelineConfigVo newConfig = new DeployPipelineConfigVo();
                    List<AutoexecParamVo> runtimeParamList = new ArrayList<>();
                    DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
                    if (config != null && CollectionUtils.isNotEmpty(config.getRuntimeParamList())) {
                        runtimeParamList = config.getRuntimeParamList();
                    }
                    newConfig.setRuntimeParamList(runtimeParamList);
                    deployAppConfigVo.setConfig(newConfig);
                }
            }
        }
        pipelineVo.setAppConfigList(appConfigList);
        return pipelineVo;
    }

}
