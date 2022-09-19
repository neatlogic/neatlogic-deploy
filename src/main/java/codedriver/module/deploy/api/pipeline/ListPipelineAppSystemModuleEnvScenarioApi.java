/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.pipeline.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        if (CollectionUtils.isNotEmpty(pipelineVo.getLaneList())) {
            for (PipelineLaneVo laneVo : pipelineVo.getLaneList()) {
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (PipelineGroupVo groupVo : laneVo.getGroupList()) {
                        if (CollectionUtils.isNotEmpty(groupVo.getJobTemplateList())) {
                            for (PipelineJobTemplateVo jobTemplateVo : groupVo.getJobTemplateList()) {
                                jobTemplateList.add(jobTemplateVo);
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

        public void add(PipelineJobTemplateVo jobTemplateVo) {
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
            if (!op.isPresent()) {
                jobTemplateList.add(existsJobVo);
            }
        }

        public List<PipelineJobTemplateVo> get() {
            return this.jobTemplateList;
        }
    }

}
