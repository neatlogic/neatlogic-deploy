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
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.pipeline.*;
import neatlogic.framework.deploy.exception.DeployPipelineNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListPipelineAppSystemModuleEnvScenarioApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Override
    public String getName() {
        return "nmdap.listpipelineappsystemmoduleenvscenarioapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/appsystemmoduleenvscenario/list";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "nmdap.listpipelineappsystemmoduleenvscenarioapi.input.param.desc.id", isRequired = true)
    })
    @Output({@Param(explode = PipelineJobTemplateVo[].class)})
    @Description(desc = "nmdap.listpipelineappsystemmoduleenvscenarioapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(id);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundException(id);
        }
        List<PipelineJobTemplateVo> jobTemplateList = new ArrayList<>();
        Map<String, DeployPipelineConfigVo> envPipelineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pipelineVo.getLaneList())) {
            for (PipelineLaneVo laneVo : pipelineVo.getLaneList()) {
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (PipelineGroupVo groupVo : laneVo.getGroupList()) {
                        if (CollectionUtils.isNotEmpty(groupVo.getJobTemplateList())) {
                            for (PipelineJobTemplateVo jobTemplateVo : groupVo.getJobTemplateList()) {
                                // 根据应用和模块合并环境和场景，用于前端展示
                                PipelineEnvScenarioVo envScenarioVo = new PipelineEnvScenarioVo();
                                envScenarioVo.setEnvId(jobTemplateVo.getEnvId());
                                envScenarioVo.setEnvName(jobTemplateVo.getEnvName());
                                envScenarioVo.setScenarioId(jobTemplateVo.getScenarioId());
                                envScenarioVo.setScenarioName(jobTemplateVo.getScenarioName());
                                Optional<PipelineJobTemplateVo> op = jobTemplateList.stream().filter(d -> d.getAppSystemId().equals(jobTemplateVo.getAppSystemId())
                                        && d.getAppModuleId().equals(jobTemplateVo.getAppModuleId())
                                ).findFirst();
                                if (op.isPresent()) {
                                    PipelineJobTemplateVo existsJobVo = op.get();
                                    existsJobVo.addEnvScenario(envScenarioVo);
                                    existsJobVo.setScenarioId(envScenarioVo.getScenarioId());
                                    DeployPipelineConfigManager.setIsJobTemplateVoHasBuildDeployType(existsJobVo, envPipelineMap);
                                } else {
                                    jobTemplateVo.addEnvScenario(envScenarioVo);
                                    DeployPipelineConfigManager.setIsJobTemplateVoHasBuildDeployType(jobTemplateVo, envPipelineMap);
                                    jobTemplateList.add(jobTemplateVo);
                                }
                            }
                        }
                    }
                }
            }
        }
        return jobTemplateList;
    }

//    static class JobTemplateList {
//        List<PipelineJobTemplateVo> jobTemplateList = new ArrayList<>();
//
//        public void add(PipelineJobTemplateVo jobTemplateVo, Map<Long, DeployPipelineConfigVo> envPipelineMap) {
//            PipelineJobTemplateVo existsJobVo = null;
//            Optional<PipelineJobTemplateVo> op = jobTemplateList.stream().filter(d -> d.getAppSystemId().equals(jobTemplateVo.getAppSystemId())
//                    && d.getAppModuleId().equals(jobTemplateVo.getAppModuleId())
//            ).findFirst();
//            existsJobVo = op.orElse(jobTemplateVo);
//            PipelineEnvScenarioVo envScenarioVo = new PipelineEnvScenarioVo();
//            envScenarioVo.setEnvId(jobTemplateVo.getEnvId());
//            envScenarioVo.setEnvName(jobTemplateVo.getEnvName());
//            envScenarioVo.setScenarioId(jobTemplateVo.getScenarioId());
//            envScenarioVo.setScenarioName(jobTemplateVo.getScenarioName());
//            existsJobVo.addEnvScenario(envScenarioVo);
//            existsJobVo.setScenarioId(envScenarioVo.getScenarioId());
//            DeployPipelineConfigManager.setIsJobTemplateVoHasBuildDeployType(existsJobVo, envPipelineMap);
//            if (!op.isPresent()) {
//                jobTemplateList.add(existsJobVo);
//            }
//        }
//
//        public List<PipelineJobTemplateVo> get() {
//            return this.jobTemplateList;
//        }
//    }

}
