package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.version.DeployVersionVo;

/**
 * @author longrf
 * @date 2022/5/26 17:20 下午
 */
public interface DeployVersionMapper {

    int checkDeployVersionIsRepeat(DeployVersionVo version);

    void insertDeployVersion(DeployVersionVo version);
}
