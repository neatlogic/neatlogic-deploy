package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/5/26 17:20 下午
 */
public interface DeployVersionMapper {

    int checkDeployVersionIsRepeat(DeployVersionVo versionVo);

    int searchDeployVersionCount(DeployVersionVo versionVo);

    int getDeployVersionBuildNoListCount(DeployVersionBuildNoVo versionBuildNoVo);

    DeployVersionVo getDeployVersionById(Long id);

    DeployVersionVo getVersionByAppSystemIdAndAppModuleIdAndVersion(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("version") String version);

    List<DeployVersionVo> searchDeployVersion(DeployVersionVo versionVo);

    List<DeployVersionBuildNoVo> searchDeployVersionBuildNoList(DeployVersionBuildNoVo versionBuildNoVo);

    Long getJobIdByDeployVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    Long getJobIdByDeployVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersionLock(DeployVersionVo versionVo);

    DeployVersionBuildNoVo getDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    int unLockDeployVersionById(@Param("id") Long id, @Param("isLocked") Long isLocked);

    Integer getDeployVersionMaxBuildNoByVersionIdLock(Long id);

    int updateDeployVersionInfoById(DeployVersionVo vo);

    int updateDeployVersionBuildNoByVersionIdAndBuildNo(DeployVersionBuildNoVo vo);

    int insertDeployVersion(DeployVersionVo versionVo);

    int deleteDeployVersionById(Long id);

    int deleteDeployVersionBuildNoByVersionId(Long versionId);

    int deleteDeployVersionEnvByVersionId(Long versionId);

    int deleteDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

}
