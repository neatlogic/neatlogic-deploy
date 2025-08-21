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

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/8/12 15:20
 **/

@Component
public class BatchDeployAuthChecker {
    private static BatchDeployAuthChecker instance;

    @Resource
    DeployJobMapper deployJobMapper;

    @Autowired
    public BatchDeployAuthChecker() {
        instance = this;
    }

    /**
     * 是否有执行权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanExecute(DeployJobVo deployJobVo) {
        return !Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())
                && Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())
                && !Objects.equals(JobStatus.RUNNING.getValue(), deployJobVo.getStatus())
                && UserContext.get().getUserUuid().equals(deployJobVo.getExecUser());
    }

    /**
     * 是否有中止权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanAbort(DeployJobVo deployJobVo) {
        return !Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus()) && Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue()) && UserContext.get().getUserUuid().equals(deployJobVo.getExecUser());
    }

    /**
     * 是否有接管权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanTakeOver(DeployJobVo deployJobVo) {
        return !Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())
                && Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())
                && !Objects.equals(deployJobVo.getExecUser(), UserContext.get().getUserUuid(true));
    }

    /**
     * 是否有编辑权限
     *
     * @param deployJobVo 批量发布作业
     * @return 是｜否
     */
    public static boolean isCanEdit(DeployJobVo deployJobVo) {
        if (!Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus()) && !Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.WAITING.getValue())) {
            return Arrays.asList(JobStatus.READY.getValue(), JobStatus.PENDING.getValue(), JobStatus.SAVED.getValue(), JobStatus.COMPLETED.getValue(), JobStatus.FAILED.getValue()).contains(deployJobVo.getStatus())
                    && (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName()));
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
            int authCount = instance.deployJobMapper.getDeployJobAuthCountByJobIdAndUuid(deployJobVo.getId(), UserContext.get().getUserUuid(true));
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
        return !Objects.equals(JobStatus.CHECKED.getValue(), deployJobVo.getStatus())
                && (Objects.equals(deployJobVo.getReviewStatus(), ReviewStatus.PASSED.getValue())
                && UserContext.get().getUserUuid().equals(deployJobVo.getExecUser()));
    }
}
