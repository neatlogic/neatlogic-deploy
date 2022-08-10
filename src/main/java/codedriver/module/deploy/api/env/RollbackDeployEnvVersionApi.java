package codedriver.module.deploy.api.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.VersionDirection;
import codedriver.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import codedriver.framework.deploy.dto.env.DeployEnvVersionVo;
import codedriver.framework.deploy.exception.DeployEnvVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployEnvVersionWhichCanRollbackNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployEnvVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RollbackDeployEnvVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Override
    public String getName() {
        return "回退环境的版本";
    }

    @Override
    public String getToken() {
        return "deploy/env/version/rollback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "环境id", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "回退环境的版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        String envName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(envId);
        if (envName == null) {
            throw new AppEnvNotFoundException(envId);
        }
        // 找到环境的当前版本，回退到当前版本第一次出现在audit表时记录的old_version
        DeployEnvVersionVo currentVersion = deployEnvVersionMapper.getDeployEnvVersionByEnvIdLock(sysId, moduleId, envId);
        if (currentVersion == null) {
            throw new DeployEnvVersionNotFoundException(envName);
        }
        DeployEnvVersionAuditVo oldestVersion = deployEnvVersionMapper.getDeployEnvOldestVersionByEnvIdAndNewVersionId(sysId, moduleId, envId, currentVersion.getVersionId());
        if (oldestVersion == null || oldestVersion.getOldVersionId() == null) {
            throw new DeployEnvVersionWhichCanRollbackNotFoundException(envName);
        }
        deployEnvVersionMapper.insertDeployEnvVersion(new DeployEnvVersionVo(sysId, moduleId, envId, oldestVersion.getOldVersionId(), oldestVersion.getOldBuildNo()));
        if (!Objects.equals(oldestVersion.getOldVersionId(), currentVersion.getVersionId())) {
            deployEnvVersionMapper.insertDeployEnvVersionAudit(new DeployEnvVersionAuditVo(sysId, moduleId, envId, oldestVersion.getOldVersionId(), currentVersion.getVersionId(), oldestVersion.getOldBuildNo(), currentVersion.getBuildNo(), VersionDirection.ROLLBACK.getValue()));
        }
        return null;
    }
}
