package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import codedriver.framework.deploy.dto.env.DeployEnvVersionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployEnvVersionMapper {

    DeployEnvVersionVo getDeployEnvVersionByEnvIdLock(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    DeployEnvVersionAuditVo getDeployEnvOldestVersionByEnvIdAndNewVersionId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("newVersionId") Long newVersionId);

    List<DeployEnvVersionAuditVo> getDeployEnvVersionAuditBySystemId(Long appSystemId);

    int insertDeployEnvVersion(DeployEnvVersionVo vo);

    int insertDeployEnvVersionAudit(DeployEnvVersionAuditVo vo);
}
