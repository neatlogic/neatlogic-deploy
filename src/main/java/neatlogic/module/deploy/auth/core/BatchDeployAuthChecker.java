/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.deploy.auth.core;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.ReviewStatus;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/8/12 15:20
 **/

@Component
public class BatchDeployAuthChecker {

    private static DeployJobMapper deployJobMapper;

    @Autowired
    public BatchDeployAuthChecker(DeployJobMapper _deployJobMapper) {
        deployJobMapper = _deployJobMapper;
    }

    /**
     * 是否有执行权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanExecute(DeployJobVo deployJobVo) {
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())) {
            if (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
                if (!Objects.equals(JobStatus.RUNNING.getValue(), deployJobVo.getStatus())) {
                    return UserContext.get().getUserUuid().equals(deployJobVo.getExecUser());
                }
            }
        }
        return false;
    }

    /**
     * 是否有接管权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanTakeOver(DeployJobVo deployJobVo) {
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())) {
            if (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
                int authCount = deployJobMapper.getDeployJobAuthCountByJobIdAndUuid(deployJobVo.getId(), UserContext.get().getUserUuid(true));
                return (authCount > 0 || AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName())) && !Objects.equals(deployJobVo.getExecUser(), UserContext.get().getUserUuid(true));
            }
        }
        return false;
    }

    /**
     * 是否有编辑权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanEdit(DeployJobVo deployJobVo) {
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())) {
            if (!Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.WAITING.getValue())) {
                return Arrays.asList(JobStatus.READY.getValue(),JobStatus.PENDING.getValue(), JobStatus.SAVED.getValue(), JobStatus.COMPLETED.getValue(), JobStatus.FAILED.getValue()).contains(deployJobVo.getStatus())
                        && (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName()) || Objects.equals(deployJobVo.getExecUser(), UserContext.get().getUserUuid(true)));
            }
        }
        return false;
    }

    /**
     * 是否有验证权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanCheck(DeployJobVo deployJobVo) {
        if (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
            int authCount = deployJobMapper.getDeployJobAuthCountByJobIdAndUuid(deployJobVo.getId(), UserContext.get().getUserUuid(true));
            return (authCount > 0 || AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName()));
        }
        return false;
    }

    /**
     * 是否有执行组权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanGroupExecute(DeployJobVo deployJobVo) {
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())) {
            if (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())) {
                return UserContext.get().getUserUuid().equals(deployJobVo.getExecUser());
            }
        }
        return false;
    }
}
