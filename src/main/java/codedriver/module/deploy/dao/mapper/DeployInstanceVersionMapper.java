package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.instance.DeployInstanceVersionAuditVo;
import codedriver.framework.deploy.dto.instance.DeployInstanceVersionVo;
import org.apache.ibatis.annotations.Param;

public interface DeployInstanceVersionMapper {

    DeployInstanceVersionVo getDeployInstanceVersionByEnvIdAndInstanceIdLock(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceId") Long instanceId);

    DeployInstanceVersionAuditVo getDeployInstanceOldestVersionByInstanceIdAndNewVersionId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceId") Long instanceId, @Param("newVersionId") Long newVersionId);

    int insertDeployInstanceVersion(DeployInstanceVersionVo vo);

    int insertDeployInstanceVersionAudit(DeployInstanceVersionAuditVo vo);
}
