package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.DeployJobVo;
import org.apache.ibatis.annotations.Param;

/**
 * @author lvzk
 * @date 2022/6/27 16:19 下午
 */
public interface DeployJobMapper {
    DeployJobVo getDeployJobByJobId(Long id);

    /**
     * 根据应用ID和模块ID查询最近一次作业的runner_map_id
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    Long getRecentlyJobRunnerMapIdByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);
}
