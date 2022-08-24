/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job.batch;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import codedriver.framework.deploy.auth.BATCHDEPLOY_VERIFY;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.job.DeployJobAuthVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.framework.deploy.dto.job.LaneVo;
import codedriver.framework.deploy.exception.DeployBatchJobCannotEditException;
import codedriver.framework.deploy.exception.DeployBatchJobNotFoundException;
import codedriver.framework.deploy.exception.DeployJobHasParentException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = BATCHDEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveBatchDeployJobApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployJobService deployJobService;


    @Override
    public String getName() {
        return "保存批量发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/save";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "作业id，不提供代表添加作业"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名称"),
            @Param(name = "saveMode", type = ApiParamType.ENUM, rule = "save,commit", isRequired = true, desc = "暂存或提交"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, rule = "manual,auto", desc = "触发方式"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "计划开始时间"),
            @Param(name = "laneList", type = ApiParamType.JSONARRAY, desc = "通道列表"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表")})
    @Output({@Param(explode = DeployJobVo.class)})
    @ResubmitInterval(3)
    @Description(desc = "保存批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (id != null) {
            DeployJobVo deployJobVo = deployJobMapper.getBatchDeployJobById(id);
            if (deployJobVo == null) {
                throw new DeployBatchJobNotFoundException(id);
            }
            //如果批量作业是running状态则不允许编辑
            if (Objects.equals(JobStatus.RUNNING.getValue(), deployJobVo.getStatus())) {
                throw new DeployBatchJobCannotEditException(JobStatus.RUNNING.getText());
            }
        }
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        String saveMode = jsonObj.getString("saveMode");
        if (saveMode.equals("save")) {
            deployJobVo.setStatus(JobStatus.SAVED.getValue());
            deployJobVo.setReviewStatus(null);
        } else {
            deployJobVo.setStatus(JobStatus.PENDING.getValue());
            if (!AuthActionChecker.check(BATCHDEPLOY_VERIFY.class)) {
                deployJobVo.setReviewStatus(ReviewStatus.WAITING.getValue());
            } else {
                deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
            }
        }
        deployJobVo.setSource(JobSource.BATCHDEPLOY.getValue());
        deployJobVo.setExecUser(UserContext.get().getUserUuid());
        if (id == null) {
            deployJobMapper.insertAutoExecJob(deployJobVo);
        } else {
            deployJobMapper.updateAutoExecJob(deployJobVo);
            deployJobMapper.deleteLaneGroupJobByJobId(deployJobVo.getId());
            deployJobMapper.resetAutoexecJobParentId(deployJobVo.getId());
            deployJobMapper.deleteJobInvokeByJobId(deployJobVo.getId());
            deployJobMapper.deleteJobAuthByJobId(deployJobVo.getId());
        }
        if (CollectionUtils.isNotEmpty(deployJobVo.getLaneList())) {
            for (int i = 0; i < deployJobVo.getLaneList().size(); i++) {
                LaneVo laneVo = deployJobVo.getLaneList().get(i);
                boolean hasLaneJob = false;
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (int j = 0; j < laneVo.getGroupList().size(); j++) {
                        LaneGroupVo groupVo = laneVo.getGroupList().get(j);
                        boolean hasGroupJob = false;
                        if (CollectionUtils.isNotEmpty(groupVo.getJobList())) {
                            hasLaneJob = true;
                            hasGroupJob = true;
                            for (int k = 0; k < groupVo.getJobList().size(); k++) {
                                DeployJobVo jobVo = groupVo.getJobList().get(k);
                                DeployJobVo checkJobVo = deployJobMapper.getJobBaseInfoById(jobVo.getId());
                                if (checkJobVo.getParentId() != null && !checkJobVo.getParentId().equals(deployJobVo.getId())) {
                                    throw new DeployJobHasParentException(checkJobVo.getName());
                                }
                                jobVo.setParentId(deployJobVo.getId());
                                deployJobMapper.updateAutoExecJobParentIdById(jobVo);
                                deployJobMapper.insertGroupJob(groupVo.getId(), jobVo.getId(), k + 1);
                                deployJobMapper.insertJobInvoke(deployJobVo.getId(), jobVo.getId(), JobSource.BATCHDEPLOY.getValue(), "deploy");
                            }
                        }
                        if (hasGroupJob) {
                            groupVo.setLaneId(laneVo.getId());
                            groupVo.setSort(j + 1);
                            groupVo.setStatus(JobStatus.PENDING.getValue());
                            deployJobMapper.insertLaneGroup(groupVo);
                        }
                    }
                }
                if (hasLaneJob) {
                    laneVo.setBatchJobId(deployJobVo.getId());
                    laneVo.setSort(i + 1);
                    laneVo.setStatus(JobStatus.PENDING.getValue());
                    deployJobMapper.insertLane(laneVo);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(deployJobVo.getAuthList())) {
            for (DeployJobAuthVo authVo : deployJobVo.getAuthList()) {
                authVo.setJobId(deployJobVo.getId());
                deployJobMapper.insertDeployJobAuth(authVo);
            }
        }
        return deployJobMapper.getBatchDeployJobById(deployJobVo.getId());
    }

}
