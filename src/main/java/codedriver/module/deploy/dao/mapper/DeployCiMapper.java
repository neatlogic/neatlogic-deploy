package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.ci.DeployCiVo;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    int insertDeployCi(DeployCiVo vo);
}
