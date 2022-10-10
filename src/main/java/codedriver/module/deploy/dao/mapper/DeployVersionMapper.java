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

    DeployVersionVo getDeployVersionBaseInfoById(Long id);

    DeployVersionVo getVersionByAppSystemIdAndAppModuleIdAndVersion(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("version") String version);

    List<DeployVersionVo> getDeployVersionBaseInfoByIdList(List<Long> idList);

    List<DeployVersionVo> getDeployVersionByIdList(@Param("idList") List<Long> idList);

    List<DeployVersionVo> getDeployVersionIncludeEnvListByVersionIdList(List<Long> idList);

    List<DeployVersionBuildNoVo> searchDeployVersionBuildNoList(DeployVersionBuildNoVo versionBuildNoVo);

    Long getJobIdByDeployVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    Long getJobIdByDeployVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    DeployVersionVo getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(DeployVersionVo versionVo);

    DeployVersionVo getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersionLock(DeployVersionVo versionVo);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersionLock(DeployVersionVo versionVo);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersion(@Param("appSystemId") Long systemId, @Param("appModuleId") Long moduleId, @Param("version") String version);

    DeployVersionVo getDeployVersionBySystemIdAndModuleIdAndVersionId(@Param("appSystemId") Long systemId, @Param("appModuleId") Long moduleId, @Param("versionId") Long versionId);

    DeployVersionBuildNoVo getDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    DeployVersionEnvVo getDeployVersionEnvByVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    DeployVersionEnvVo getDeployVersionEnvByVersionIdAndEnvIdAndBuildNo(@Param("versionId") Long versionId, @Param("envId") Long envId, @Param("buildNo") Integer buildNo);

    List<Long> getDeployVersionIdList(DeployVersionVo versionVo);

    List<DeployVersionDependencyVo> getDeployVersionDependencyListByVersionId(Long versionId);

    DeployVersionDependencyVo getDeployVersionDependencyByVersionIdAndPackageId(@Param("versionId") Long versionId, @Param("packageId") Long packageId);

    String getDeployVersionAppbuildCredentialByProxyToUrl(String redirectUrl);

    List<DeployVersionEnvInstanceVo> getDeployedInstanceByVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    List<DeployVersionVo> getDeployVersionBySystemId(Long systemId);

    int unFreezeDeployVersionById(@Param("id") Long id, @Param("isFreeze") Long isFreeze);

    int updateDeployVersionDependencyBuildTimeById(Long id);

    Integer getDeployVersionMaxBuildNoByVersionIdLock(Long id);

    int updateDeployVersionInfoById(DeployVersionVo vo);

    int updateDeployVersionBuildNoByVersionIdAndBuildNo(DeployVersionBuildNoVo vo);

    int updateDeployVersionEnvInfo(DeployVersionEnvVo vo);

    int insertDeployVersion(DeployVersionVo versionVo);

    int insertDeployVersionEnv(DeployVersionEnvVo vo);

    int insertDeployVersionBuildNo(DeployVersionBuildNoVo deployVersionBuildNoVo);

    int insertDeployVersionBuildQuality(DeployVersionBuildQualityVo vo);

    int insertDeployVersionUnitTest(DeployVersionUnitTestVo vo);

    int insertDeployVersionDependency(DeployVersionDependencyVo vo);

    int insertDeployedInstance(DeployVersionEnvInstanceVo vo);

    int deleteDeployVersionById(Long id);

    int deleteDeployVersionBuildNoByVersionId(Long versionId);

    int deleteDeployVersionEnvByVersionId(Long versionId);

    int deleteDeployVersionBuildNoByVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    int deleteDeployVersionDependencyByVersionIdAndPackageIdList(@Param("versionId") Long versionId, @Param("packageIdList") List<Long> packageIdList);

    int deleteDeployVersionDependencyByVersionId(@Param("versionId") Long versionId);

    int deleteDeployedInstanceByVersionId(Long versionId);

    int deleteDeployVersionBuildQualityByVersionId(Long versionId);

    int deleteDeployVersionUnitTestByVersionId(Long versionId);

}
