package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.VersionEnvStatus;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployVersionEnvForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.updatedeployversionenvforautoexecapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/env/update/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "runnerId", desc = "term.deploy.runnerid", type = ApiParamType.LONG),
            @Param(name = "runnerGroup", desc = "nmdav.updatedeployversionenvforautoexecapi.input.param.desc.runnergroup", type = ApiParamType.JSONOBJECT),
            @Param(name = "jobId", desc = "term.autoexec.jobid", type = ApiParamType.LONG),
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "term.cmdb.envid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "nmdav.updatedeployversionenvforautoexecapi.input.param.desc.buildno", type = ApiParamType.INTEGER),
            @Param(name = "isMirror", desc = "nmdav.updatedeployversionenvforautoexecapi.input.param.desc.ismirror", rule = "0,1", type = ApiParamType.ENUM),
            @Param(name = "status", desc = "nmdav.updatedeployversionenvforautoexecapi.input.param.desc.status", member = VersionEnvStatus.class, type = ApiParamType.ENUM),
    })
    @Description(desc = "nmdav.updatedeployversionenvforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long runnerId = paramObj.getLong("runnerId");
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        String version = paramObj.getString("version");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        deployVersionMapper.getDeployVersionLockById(versionVo.getId());
        DeployVersionEnvVo envVo = paramObj.toJavaObject(DeployVersionEnvVo.class);
        envVo.setVersionId(versionVo.getId());
        envVo.setRunnerMapId(runnerId);
        if (deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(), envId) == null) {
            if (runnerId == null) {
                throw new ParamNotExistsException("runnerId");
            }
            if (envVo.getJobId() == null) {
                throw new ParamNotExistsException("jobId");
            }
            if (StringUtils.isBlank(envVo.getStatus())) {
                throw new ParamNotExistsException("status");
            }
            deployVersionMapper.insertDeployVersionEnv(envVo);
        } else {
            deployVersionMapper.updateDeployVersionEnvInfo(envVo);
        }
        return null;
    }

}
