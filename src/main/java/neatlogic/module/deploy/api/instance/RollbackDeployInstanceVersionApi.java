package neatlogic.module.deploy.api.instance;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.VersionDirection;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionAuditVo;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionVo;
import neatlogic.framework.deploy.exception.DeployInstanceInEnvNotFoundException;
import neatlogic.framework.deploy.exception.DeployInstanceVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployInstanceVersionWhichCanRollbackNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployInstanceVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RollbackDeployInstanceVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

    @Resource
    private DeployResourceMapper deployResourceMapper;

    @Override
    public String getName() {
        return "nmdai.rollbackdeployinstanceversionapi.getname";
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
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "term.cmdb.envid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "resourceId", desc = "nmdai.rollbackdeployinstanceversionapi.input.param.desc.resourceid", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "nmdai.rollbackdeployinstanceversionapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }
        String envName = env.getName();
        List<Long> instanceIdList = deployResourceMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(sysId, moduleId, envId));
        if (CollectionUtils.isEmpty(instanceIdList) || !instanceIdList.contains(resourceId)) {
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
