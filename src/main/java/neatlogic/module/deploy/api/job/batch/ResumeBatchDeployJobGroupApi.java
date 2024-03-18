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

package neatlogic.module.deploy.api.job.batch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.job.LaneGroupVo;
import neatlogic.framework.deploy.exception.DeployBatchJobCannotExecuteException;
import neatlogic.framework.deploy.exception.DeployBatchJobGroupNotFoundException;
import neatlogic.framework.deploy.exception.DeployBatchJobNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.auth.core.BatchDeployAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployBatchJobMapper;
import neatlogic.module.deploy.service.DeployBatchJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/8/5 11:20
 **/

@Transactional
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ResumeBatchDeployJobGroupApi extends PrivateApiComponentBase {
    @Resource
    private DeployBatchJobService deployBatchJobService;
    @Resource
    private DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public String getName() {
        return "继续执行批量发布作业组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "批量作业组id"),
            @Param(name = "batchJobAction", type = ApiParamType.ENUM, rule = "refireAll,refireResetAll", isRequired = true, desc = "批量作业执行策略，refireAll：跳过所有已完成的子作业；refireResetAll:执行所有子作业"),
            @Param(name = "jobAction", type = ApiParamType.ENUM, rule = "refireAll,refireResetAll", isRequired = true, desc = "子作业执行策略，refireAll：跳过所有已完成、已忽略的节点；refireResetAll:执行所有节点")
    })
    @Description(desc = "继续执行批量发布作业组接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long currentGroupId = jsonObj.getLong("id");
        LaneGroupVo currentGroup = deployBatchJobMapper.getLaneGroupByGroupId(currentGroupId);
        if (currentGroup == null) {
            throw new DeployBatchJobGroupNotFoundException(currentGroupId);
        }
        DeployJobVo batchJobVo = deployBatchJobMapper.getBatchJobByGroupId(currentGroupId);
        if (batchJobVo == null) {
            throw new DeployBatchJobNotFoundException();
        }
        if (!BatchDeployAuthChecker.isCanGroupExecute(batchJobVo)) {
            throw new DeployBatchJobCannotExecuteException();
        }
        currentGroup.setBatchJobAction(jsonObj.getString("batchJobAction"));
        currentGroup.setJobAction(jsonObj.getString("jobAction"));
        Long nextGroupId = deployBatchJobMapper.getNextGroupId(currentGroup.getLaneId(), currentGroup.getSort());
        if (nextGroupId != null) {
            deployBatchJobService.fireLaneNextGroup(currentGroup, nextGroupId, new JSONObject());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/resume/group";
    }

}
