package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.DeployJobVo;

/**
 * @author lvzk
 * @date 2022/6/27 16:19 下午
 */
public interface DeployJobMapper {
    DeployJobVo getDeployJobByJobId(Long id);

    Integer insertDeployJob(DeployJobVo deployJobVo);
}
