package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionAuditVo;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployInstanceVersionMapper {

    DeployInstanceVersionVo getDeployInstanceVersionByEnvIdAndInstanceIdLock(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceId") Long instanceId);

    List<DeployInstanceVersionVo> getDeployInstanceVersionByEnvIdAndInstanceIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceIdList") List<Long> instanceIdList);

    DeployInstanceVersionAuditVo getDeployInstanceOldestVersionByInstanceIdAndNewVersionId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceId") Long instanceId, @Param("newVersionId") Long newVersionId);

    int insertDeployInstanceVersion(DeployInstanceVersionVo vo);

    int insertDeployInstanceVersionAudit(DeployInstanceVersionAuditVo vo);

    int deleteDeployInstanceVersionByVersionId(Long versionId);

    int deleteDeployInstanceVersionAuditByVersionId(Long versionId);
}
