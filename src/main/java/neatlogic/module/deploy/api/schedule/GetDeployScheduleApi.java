/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.deploy.api.schedule;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.exception.schedule.DeployScheduleNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private DeployPipelineMapper pipelineMapper;

    @Override
    public String getToken() {
        return "deploy/schedule/get";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "获取定时作业信息")
    @Output({
            @Param(name = "Return", explode = DeployScheduleVo.class, desc = "定时作业信息")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleById(id);
        if (scheduleVo == null) {
            throw new DeployScheduleNotFoundEditTargetException(id);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            Long appSystemId = scheduleVo.getAppSystemId();
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            Set<String> actionSet = DeployAppAuthChecker.builder(appSystemId)
                    .addEnvAction(config.getEnvId())
                    .addScenarioAction(config.getScenarioId())
                    .check();
            if (actionSet.contains(config.getEnvId().toString()) && actionSet.contains(config.getScenarioId().toString())) {
                scheduleVo.setEditable(1);
                scheduleVo.setDeletable(1);
            }
        } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
            String pipelineType = scheduleVo.getPipelineType();
            if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                Set<String> actionSet = DeployAppAuthChecker.builder(scheduleVo.getAppSystemId())
                        .addOperationAction(DeployAppConfigAction.PIPELINE.getValue())
                        .check();
                if (actionSet.contains(DeployAppConfigAction.PIPELINE.getValue())) {
                    scheduleVo.setEditable(1);
                    scheduleVo.setDeletable(1);
                } else {
                    List<Long> pipelineIdList = new ArrayList<>();
                    pipelineIdList.add(scheduleVo.getPipelineId());
                    pipelineIdList = pipelineMapper.checkHasAuthPipelineIdList(pipelineIdList, userUuid);
                    if (CollectionUtils.isNotEmpty(pipelineIdList)) {
                        scheduleVo.setEditable(1);
                        scheduleVo.setDeletable(1);
                    }
                }
            } else if (pipelineType.equals(PipelineType.GLOBAL.getValue())) {
                boolean hasPipelineModify = AuthActionChecker.check(PIPELINE_MODIFY.class);
                if (hasPipelineModify) {
                    scheduleVo.setEditable(1);
                    scheduleVo.setDeletable(1);
                } else {
                    List<Long> pipelineIdList = new ArrayList<>();
                    pipelineIdList.add(scheduleVo.getPipelineId());
                    pipelineIdList = pipelineMapper.checkHasAuthPipelineIdList(pipelineIdList, userUuid);
                    if (CollectionUtils.isNotEmpty(pipelineIdList)) {
                        scheduleVo.setEditable(1);
                        scheduleVo.setDeletable(1);
                    }
                }
            }
        }
        return scheduleVo;
    }
}
