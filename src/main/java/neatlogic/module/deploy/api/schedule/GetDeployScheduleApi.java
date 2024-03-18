/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.schedule;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.exception.schedule.DeployScheduleNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
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
        return "nmaas.autoexecschedulegetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id")
    })
    @Description(desc = "nmaas.autoexecschedulegetapi.getname")
    @Output({
            @Param(name = "Return", explode = DeployScheduleVo.class, desc = "term.deploy.scheduleinfo")
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
