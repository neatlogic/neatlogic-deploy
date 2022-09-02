package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.ci.DeployCiVo;

import java.util.List;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    DeployCiVo getDeployCiById(Long id);

    int searchDeployCiCount(DeployCiVo vo);

    List<Long> searchDeployCiIdList(DeployCiVo vo);

    List<DeployCiVo> getDeployCiListByIdList(List<Long> list);

    int insertDeployCi(DeployCiVo vo);
}
