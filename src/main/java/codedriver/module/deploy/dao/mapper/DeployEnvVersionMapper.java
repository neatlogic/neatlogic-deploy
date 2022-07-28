package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import codedriver.framework.deploy.dto.env.DeployEnvVersionVo;

public interface DeployEnvVersionMapper {

    int insertDeployEnvVersion(DeployEnvVersionVo vo);

    int insertDeployEnvVersionAudit(DeployEnvVersionAuditVo vo);
}
