package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.DeployPackageVo;
import org.apache.ibatis.annotations.Param;

public interface DeployPackageMapper {

    DeployPackageVo getPackageByGroupIdAndArtifactIdAndVersion(@Param("groupId") String groupId, @Param("artifactId") String artifactId, @Param("version") String version);

    int insertPackage(DeployPackageVo vo);

}
