package codedriver.module.deploy.api.instance;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.VersionDirection;
import codedriver.framework.deploy.dto.instance.DeployInstanceVersionAuditVo;
import codedriver.framework.deploy.dto.instance.DeployInstanceVersionVo;
import codedriver.framework.deploy.exception.DeployInstanceInEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployInstanceVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployInstanceVersionWhichCanRollbackNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployInstanceVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RollbackDeployInstanceVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

    @Override
    public String getName() {
        return "回退实例的版本";
    }

    @Override
    public String getToken() {
        return "deploy/instance/version/rollback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "环境id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "resourceId", desc = "实例id", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "回退实例的版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        Long resourceId = paramObj.getLong("resourceId");
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        String envName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(envId);
        if (envName == null) {
            throw new AppEnvNotFoundException(envId);
        }
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(sysId, moduleId, envId));
        if (instanceIdList.size() == 0 || !instanceIdList.contains(resourceId)) {
            throw new DeployInstanceInEnvNotFoundException(paramObj.getString("sysName"), paramObj.getString("moduleName"), envName, resourceId);
        }
        // 找到实例的当前版本，回退到当前版本第一次出现在audit表时记录的old_version
        DeployInstanceVersionVo currentVersion = deployInstanceVersionMapper.getDeployInstanceVersionByEnvIdAndInstanceIdLock(sysId, moduleId, envId, resourceId);
        if (currentVersion == null) {
            throw new DeployInstanceVersionNotFoundException(resourceId);
        }
        DeployInstanceVersionAuditVo oldestVersion = deployInstanceVersionMapper.getDeployInstanceOldestVersionByInstanceIdAndNewVersionId(sysId, moduleId, envId, resourceId, currentVersion.getVersionId());
        if (oldestVersion == null || oldestVersion.getOldVersionId() == null) {
            throw new DeployInstanceVersionWhichCanRollbackNotFoundException(resourceId);
        }
        deployInstanceVersionMapper.insertDeployInstanceVersion(new DeployInstanceVersionVo(sysId, moduleId, envId, resourceId, oldestVersion.getOldVersionId(), oldestVersion.getOldBuildNo()));
        if (!Objects.equals(oldestVersion.getOldVersionId(), currentVersion.getVersionId())) {
            deployInstanceVersionMapper.insertDeployInstanceVersionAudit(new DeployInstanceVersionAuditVo(sysId, moduleId, envId, resourceId, oldestVersion.getOldVersionId(), currentVersion.getVersionId(), oldestVersion.getOldBuildNo(), currentVersion.getBuildNo(), VersionDirection.ROLLBACK.getValue()));
        }
        return null;
    }
}
