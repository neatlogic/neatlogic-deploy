/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.job.*;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lvzk
 * @date 2022/6/27 16:19 下午
 */
public interface DeployJobMapper {
    DeployJobVo getDeployJobByJobId(Long id);

    List<DeployJobVo> getDeployJobByJobIdList(List<Long> idList);

    int searchDeployJobCount(DeployJobVo deployJobVo);

    List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo);

    List<Long> getJobIdListByParentId(Long parentId);

    List<Long> searchDeployJobId(DeployJobVo deployJobVo);

    /**
     * 根据应用ID和模块ID查询最近一次作业的runner_map_id
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    Long getRecentlyJobRunnerMapIdByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployJobVo> getDeployJobListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployJobVo> getDeployJobListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    DeployJobContentVo getDeployJobContentLock(String contentHash);

    DeployJobVo getBatchDeployJobById(Long id);

    List<DeployJobVo> getDeployJobByIdList(@Param("idList") List<Long> idList);

    DeployJobContentVo getDeployJobContent(String contentHash);

    void insertDeployJob(DeployJobVo deployJobVo);

    void insertAutoExecJob(DeployJobVo deployJobVo);


    void insertIgnoreDeployJobContent(DeployJobContentVo deployJobContentVo);

    void insertLane(LaneVo laneVo);

    void insertLaneGroup(LaneGroupVo laneGroupVo);

    void insertDeployJobAuth(DeployJobAuthVo deployJobAuthVo);

    void insertGroupJob(@Param("groupId") Long groupId, @Param("jobId") Long jobId, @Param("sort") Integer sort);

    void insertJobInvoke(@Param("jobId") Long jobId, @Param("invokeId") Long invokeId, @Param("source") String source, @Param("type") String type);

    void insertDeployVersionBuildNo(DeployVersionBuildNoVo deployVersionBuildNoVo);

    void updateAutoExecJob(DeployJobVo deployJobVo);

    void resetAutoexecJobParentId(Long jobId);

    void updateDeployJobReviewStatusById(DeployJobVo deployJobVo);

    void updateDeployJobRunnerMapId(@Param("jobId") Long jobId, @Param("runnerMapId") Long runnerMapId);

    void updateAutoExecJobParentIdById(DeployJobVo deployJobVo);

    void deleteDeployJobContentByHash(String contentHash);

    void deleteLaneGroupJobByJobId(Long jobId);

    void deleteJobAuthByJobId(Long jobId);

    void deleteJobInvokeByJobId(Long jobId);

    void deleteJobById(Long jobId);

}
