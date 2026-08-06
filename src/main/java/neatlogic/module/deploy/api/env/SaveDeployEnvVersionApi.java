package neatlogic.module.deploy.api.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.VersionDirection;
import neatlogic.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import neatlogic.framework.deploy.dto.env.DeployEnvVersionVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionBuildNoNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployEnvVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

@Service
@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployEnvVersionApi extends PrivateApiComponentBase {
    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "nmdae.savedeployenvversionapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/env/version/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "term.cmdb.envid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "nmdae.savedeployenvversionapi.input.param.desc.buildno", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "execUser", desc = "nmdae.savedeployenvversionapi.input.param.desc.execuser", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "deployTime", desc = "nmdae.savedeployenvversionapi.input.param.desc.deploytime", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "nmdae.savedeployenvversionapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        String execUser = paramObj.getString("execUser");
        Long deployTime = paramObj.getLong("deployTime");
        if (userMapper.checkUserIsExists(execUser) == 0 && !Objects.equals(execUser, SystemUser.SYSTEM.getUserUuid())) {
            throw new UserNotFoundException(execUser);
        }
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        if (deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo) == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        DeployEnvVersionVo currentVersion = deployEnvVersionMapper.getDeployEnvVersionByEnvIdLock(sysId, moduleId, envId);
        deployEnvVersionMapper.insertDeployEnvVersion(new DeployEnvVersionVo(sysId, moduleId, envId, versionVo.getId(), buildNo, execUser, new Date(deployTime * 1000)));
        Long oldVersionId = null;
        Integer oldBuildNo = null;
        if (currentVersion != null) {
            oldVersionId = currentVersion.getVersionId();
            oldBuildNo = currentVersion.getBuildNo();
        }
        if (!Objects.equals(versionVo.getId(), oldVersionId)) {
            deployEnvVersionMapper.insertDeployEnvVersionAudit(new DeployEnvVersionAuditVo(sysId, moduleId, envId, versionVo.getId(), oldVersionId, buildNo, oldBuildNo, VersionDirection.FORWARD.getValue()));
        }
        return null;
    }
}
