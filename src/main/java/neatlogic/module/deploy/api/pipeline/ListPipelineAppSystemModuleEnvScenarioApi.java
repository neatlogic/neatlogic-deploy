/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.pipeline;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.pipeline.*;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
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
        return "获取超级流水线应用模块环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/appsystemmoduleenvscenario/list";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)
    })
    @Output({@Param(explode = PipelineJobTemplateVo[].class)})
    @Description(desc = "获取超级流水线应用模块环境列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        PipelineVo pipelineVo = deployPipelineMapper.getPipelineById(jsonObj.getLong("id"));
        JobTemplateList jobTemplateList = new JobTemplateList();
        Map<Long, DeployPipelineConfigVo> envPipelineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pipelineVo.getLaneList())) {
            for (PipelineLaneVo laneVo : pipelineVo.getLaneList()) {
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (PipelineGroupVo groupVo : laneVo.getGroupList()) {
                        if (CollectionUtils.isNotEmpty(groupVo.getJobTemplateList())) {
                            for (PipelineJobTemplateVo jobTemplateVo : groupVo.getJobTemplateList()) {
                                jobTemplateList.add(jobTemplateVo, envPipelineMap);
                            }
                        }
                    }
                }
            }
        }
        return jobTemplateList.get();
    }

    static class JobTemplateList {
        List<PipelineJobTemplateVo> jobTemplateList = new ArrayList<>();

        public void add(PipelineJobTemplateVo jobTemplateVo, Map<Long, DeployPipelineConfigVo> envPipelineMap) {
            PipelineJobTemplateVo existsJobVo = null;
            Optional<PipelineJobTemplateVo> op = jobTemplateList.stream().filter(d -> d.getAppSystemId().equals(jobTemplateVo.getAppSystemId())
                    && d.getAppModuleId().equals(jobTemplateVo.getAppModuleId())
            ).findFirst();
            existsJobVo = op.orElse(jobTemplateVo);
            PipelineEnvScenarioVo envScenarioVo = new PipelineEnvScenarioVo();
            envScenarioVo.setEnvId(jobTemplateVo.getEnvId());
            envScenarioVo.setEnvName(jobTemplateVo.getEnvName());
            envScenarioVo.setScenarioId(jobTemplateVo.getScenarioId());
            envScenarioVo.setScenarioName(jobTemplateVo.getScenarioName());
            existsJobVo.addEnvScenario(envScenarioVo);
            existsJobVo.setScenarioId(envScenarioVo.getScenarioId());
            DeployPipelineConfigManager.setIsJobTemplateVoHasBuildDeployType(existsJobVo, envPipelineMap);
            if (!op.isPresent()) {
                jobTemplateList.add(existsJobVo);
            }
        }

        public List<PipelineJobTemplateVo> get() {
            return this.jobTemplateList;
        }
    }

}
