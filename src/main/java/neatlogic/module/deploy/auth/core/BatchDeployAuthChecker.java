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
