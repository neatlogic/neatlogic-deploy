/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.job.DeployJobContentVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lvzk
 * @date 2022/6/27 16:19 下午
 */
public interface DeployJobMapper {
    DeployJobVo getDeployJobByJobId(Long id);

    int searchDeployJobCount(DeployJobVo deployJobVo);

    List<DeployJobVo> searchDeployJob(DeployJobVo deployJobVo);

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

    DeployJobContentVo getDeployJobContentLock(String contentHash);

    DeployJobContentVo getDeployJobContent(String contentHash);

    Integer insertDeployJob(DeployJobVo deployJobVo);

    Integer insertIgnoreDeployJobContent(DeployJobContentVo deployJobContentVo);

    Integer insertDeployVersionBuildNo(DeployVersionBuildNoVo deployVersionBuildNoVo);

    Integer updateDeployJobRunnerMapId(DeployJobVo deployJobVo);

    Integer deleteDeployJobContentByHash(String contentHash);

}
