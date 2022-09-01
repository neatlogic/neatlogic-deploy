package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.ci.DeployCiVo;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    DeployCiVo getDeployCiById(Long id);

    int insertDeployCi(DeployCiVo vo);
}
