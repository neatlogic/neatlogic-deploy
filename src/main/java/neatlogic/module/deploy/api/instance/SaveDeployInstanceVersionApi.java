package neatlogic.module.deploy.api.instance;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.VersionDirection;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionAuditVo;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvInstanceVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployInstanceInEnvNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionBuildNoNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployInstanceVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployInstanceVersionApi extends PrivateApiComponentBase {
    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "设置实例的版本号";
    }

    @Override
    public String getToken() {
        return "deploy/instance/version/save";
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
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "编译号", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "execUser", desc = "发布用户", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "deployTime", desc = "发布时间（时间戳为秒数）", isRequired = true, type = ApiParamType.LONG),
    })
    @Output({
    })
    @Description(desc = "设置实例的版本号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        Long resourceId = paramObj.getLong("resourceId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        String execUser = paramObj.getString("execUser");
        Long deployTime = paramObj.getLong("deployTime");
        Date lcd = new Date(deployTime * 1000);
        if (userMapper.checkUserIsExists(execUser) == 0 && !Objects.equals(execUser, SystemUser.SYSTEM.getUserUuid())) {
            throw new UserNotFoundException(execUser);
        }
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
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        if (deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo) == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        DeployInstanceVersionVo currentVersion = deployInstanceVersionMapper.getDeployInstanceVersionByEnvIdAndInstanceIdLock(sysId, moduleId, envId, resourceId);
        deployInstanceVersionMapper.insertDeployInstanceVersion(new DeployInstanceVersionVo(sysId, moduleId, envId, resourceId, versionVo.getId(), buildNo, execUser, lcd));
        Long oldVersionId = null;
        Integer oldBuildNo = null;
        if (currentVersion != null) {
            oldVersionId = currentVersion.getVersionId();
            oldBuildNo = currentVersion.getBuildNo();
        }
        if (!Objects.equals(versionVo.getId(), oldVersionId)) {
            deployInstanceVersionMapper.insertDeployInstanceVersionAudit(new DeployInstanceVersionAuditVo(sysId, moduleId, envId, resourceId, versionVo.getId(), oldVersionId, buildNo, oldBuildNo, VersionDirection.FORWARD.getValue()));
        }
        deployVersionMapper.insertDeployedInstance(new DeployVersionEnvInstanceVo(resourceId, versionVo.getId(), envId, execUser, lcd));
        return null;
    }
}
