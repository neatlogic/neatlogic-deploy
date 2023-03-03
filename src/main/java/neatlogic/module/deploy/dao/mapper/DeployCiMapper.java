package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.ci.DeployCiAuditVo;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployCiMapper {

    int checkDeployCiIsRepeat(DeployCiVo vo);

    DeployCiVo getDeployCiById(Long id);

    DeployCiVo getDeployCiLockById(Long id);

    int searchDeployCiCount(DeployCiVo vo);

    List<DeployCiVo> searchDeployCiList(DeployCiVo vo);

    List<DeployCiVo> getDeployCiListByRepoServerAddressAndRepoNameAndEvent(@Param("ipList") List<String> ipList, @Param("repoName") String repoName, @Param("event") String event);

    int searchDeployCiAuditCount(DeployCiAuditVo vo);

    List<DeployCiAuditVo> searchDeployCiAudit(DeployCiAuditVo vo);

    int updateDeployActiveStatus(@Param("id") Long id, @Param("isActive") Integer isActive);

    int insertDeployCi(DeployCiVo vo);

    int insertDeployCiAudit(DeployCiAuditVo vo);

    int deleteDeployCiById(Long id);

    int deleteDeployCiAuditByCiId(Long id);
}
