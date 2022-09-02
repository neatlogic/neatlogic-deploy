package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
import codedriver.framework.deploy.dto.ci.DeployCiVo;

import java.util.List;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    DeployCiVo getDeployCiById(Long id);

    int searchDeployCiCount(DeployCiVo vo);

    List<Long> searchDeployCiIdList(DeployCiVo vo);

    List<DeployCiVo> getDeployCiListByIdList(List<Long> list);

    int searchDeployCiAuditCount(DeployCiAuditVo vo);

    List<DeployCiAuditVo> searchDeployCiAudit(DeployCiAuditVo vo);

    int insertDeployCi(DeployCiVo vo);

    int insertDeployCiJobAudit(DeployCiAuditVo vo);
}
