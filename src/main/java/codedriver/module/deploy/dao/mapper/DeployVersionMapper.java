package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.version.*;
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

    List<DeployVersionVo> getDeployVersionByIdList(List<Long> idList);

    List<DeployVersionBuildNoVo> searchDeployVersionBuildNoList(DeployVersionBuildNoVo versionBuildNoVo);

    Long getJobIdByDeployVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    Long getJobIdByDeployVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersion(DeployVersionVo versionVo);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersionLock(DeployVersionVo versionVo);

    DeployVersionBuildNoVo getDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    DeployVersionEnvVo getDeployVersionEnvByVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    List<Long> getDeployVersionIdList(DeployVersionVo versionVo);

    List<DeployVersionDependencyVo> getDeployVersionDependencyListByVersionId(Long versionId);

    DeployVersionDependencyVo getDeployVersionDependencyByVersionIdAndPackageId(@Param("versionId") Long versionId, @Param("packageId") Long packageId);

    int unFreezeDeployVersionById(@Param("id") Long id, @Param("isFreeze") Long isFreeze);

    int updateDeployVersionDependencyBuildTimeById(Long id);

    Integer getDeployVersionMaxBuildNoByVersionIdLock(Long id);

    int updateDeployVersionInfoById(DeployVersionVo vo);

    int updateDeployVersionBuildNoByVersionIdAndBuildNo(DeployVersionBuildNoVo vo);

    int updateDeployVersionEnvInfo(DeployVersionEnvVo vo);

    int insertDeployVersion(DeployVersionVo versionVo);

    int insertDeployVersionEnv(DeployVersionEnvVo vo);

    int insertDeployVersionBuildQuality(DeployVersionBuildQualityVo vo);

    int insertDeployVersionBuildQualityLog(DeployVersionBuildQualityVo vo);

    int insertDeployVersionDependency(DeployVersionDependencyVo vo);

    int deleteDeployVersionById(Long id);

    int deleteDeployVersionBuildNoByVersionId(Long versionId);

    int deleteDeployVersionEnvByVersionId(Long versionId);

    int deleteDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    int deleteDeployVersionDependencyByVersionIdAndPackageIdList(@Param("versionId") Long versionId, @Param("packageIdList") List<Long> packageIdList);

}
