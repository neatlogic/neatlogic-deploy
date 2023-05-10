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

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.exception.DeployScheduleNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import neatlogic.module.deploy.schedule.plugin.DeployJobScheduleJob;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getToken() {
        return "deploy/schedule/delete";
    }

    @Override
    public String getName() {
        return "删除定时作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "删除定时作业")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleById(id);
        if (scheduleVo == null) {
            throw new DeployScheduleNotFoundException(id);
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        IJob jobHandler = SchedulerManager.getHandler(DeployJobScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(DeployJobScheduleJob.class.getName());
        }
        JobObject jobObject = new JobObject.Builder(scheduleVo.getUuid(), jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid).build();
        schedulerManager.unloadJob(jobObject);
        deployScheduleMapper.deleteScheduleById(id);
        return null;
    }

}
