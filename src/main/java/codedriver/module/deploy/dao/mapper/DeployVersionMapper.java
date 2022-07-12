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

    List<DeployVersionVo> searchDeployVersion(DeployVersionVo versionVo);

    List<DeployVersionBuildNoVo> searchDeployVersionBuildNoList(DeployVersionBuildNoVo versionBuildNoVo);

    Long getJobIdByDeployVersionIdAndBuildNo(@Param("versionId") Long versionId, @Param("buildNo") Integer buildNo);

    Long getJobIdByDeployVersionIdAndEnvId(@Param("versionId") Long versionId, @Param("envId") Long envId);

    void unLockDeployVersionById(@Param("id") Long id, @Param("isLocked") Long isLocked);

    void insertDeployVersion(DeployVersionVo versionVo);

    void deleteDeployVersionById(Long id);

}
