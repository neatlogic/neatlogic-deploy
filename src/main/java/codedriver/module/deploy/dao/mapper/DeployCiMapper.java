package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    DeployCiVo getDeployCiById(Long id);

    int searchDeployCiCount(DeployCiVo vo);

    List<Long> searchDeployCiIdList(DeployCiVo vo);

    List<DeployCiVo> getDeployCiListByIdList(List<Long> list);

    int searchDeployCiAuditCount(DeployCiAuditVo vo);

    List<DeployCiAuditVo> searchDeployCiAudit(DeployCiAuditVo vo);

    int updateDeployActiveStatus(@Param("id") Long id, @Param("isActive") Integer isActive);

    int insertDeployCi(DeployCiVo vo);

    int insertDeployCiJobAudit(DeployCiAuditVo vo);

    int deleteDeployCiById(Long id);

    int deleteDeployCiAuditByCiId(Long id);
}
