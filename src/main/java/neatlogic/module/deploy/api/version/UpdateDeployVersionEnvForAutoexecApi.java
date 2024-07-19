package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.VersionEnvStatus;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployJobNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployVersionEnvForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Override
    public String getName() {
        return "更新发布版本环境信息";
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
            @Param(name = "runnerId", desc = "runnerId", type = ApiParamType.LONG),
            @Param(name = "runnerGroup", desc = "runnerGroup", type = ApiParamType.JSONOBJECT),
            @Param(name = "jobId", desc = "作业ID", type = ApiParamType.LONG),
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "环境id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "isMirror", desc = "是否镜像发布", rule = "0,1", type = ApiParamType.ENUM),
            @Param(name = "status", desc = "环境状态", member = VersionEnvStatus.class, type = ApiParamType.ENUM),
    })
    @Description(desc = "更新发布版本环境信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long runnerId = paramObj.getLong("runnerId");
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        String version = paramObj.getString("version");
        Long jobId = paramObj.getLong("jobId");
        String buildNo = paramObj.getString("buildNo");
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobId);
        if (deployJobVo == null) {
            throw new DeployJobNotFoundException(jobId);
        }
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        deployVersionMapper.getDeployVersionLockById(versionVo.getId());
        deployJobMapper.updateDeployJobBuildNoById(jobId, buildNo);
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
