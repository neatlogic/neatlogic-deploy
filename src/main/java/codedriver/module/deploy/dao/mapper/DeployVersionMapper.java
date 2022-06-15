package codedriver.module.deploy.dao.mapper;

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

    void insertDeployVersion(DeployVersionVo versionVo);

    void unLockDeployVersionById(@Param("id") Long id, @Param("isLocked") Long isLocked);

    void deleteDeployVersionById(Long id);

    DeployVersionVo getDeployVersionById(Long id);

    List<DeployVersionVo> searchDeployVersion(DeployVersionVo versionVo);

}
