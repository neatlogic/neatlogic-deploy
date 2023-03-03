package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import neatlogic.framework.deploy.dto.env.DeployEnvVersionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployEnvVersionMapper {

    DeployEnvVersionVo getDeployEnvVersionByEnvIdLock(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    List<DeployEnvVersionVo> getDeployEnvVersionBySystemId(Long appSystemId);

    DeployEnvVersionAuditVo getDeployEnvOldestVersionByEnvIdAndNewVersionId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("newVersionId") Long newVersionId);

    List<DeployEnvVersionAuditVo> getDeployEnvVersionAuditBySystemId(Long appSystemId);

    List<DeployEnvVersionAuditVo> getDeployEnvVersionAuditBySystemIdAndModueIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    int insertDeployEnvVersion(DeployEnvVersionVo vo);

    int insertDeployEnvVersionAudit(DeployEnvVersionAuditVo vo);

    int deleteDeployEnvVersionByVersionId(Long versionId);

    int deleteDeployEnvVersionAuditByVersionId(Long versionId);
}
