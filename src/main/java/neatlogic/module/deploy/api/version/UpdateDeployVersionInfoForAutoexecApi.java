package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.BuildNoStatus;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionBuildNoNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployVersionInfoForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "更新发布版本配置";
    }

    @Override
    public String getToken() {
        return "deploy/version/info/update/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "runnerId", desc = "runnerId", type = ApiParamType.LONG),
            @Param(name = "runnerGroup", desc = "runnerGroup", type = ApiParamType.JSONOBJECT),
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "buildNo", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "verInfo", desc = "版本信息", isRequired = true, type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "更新发布版本配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long runnerId = paramObj.getLong("runnerId");
        JSONObject runnerGroup = paramObj.getJSONObject("runnerGroup");
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        JSONObject verInfo = paramObj.getJSONObject("verInfo");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        deployVersionMapper.getDeployVersionLockById(versionVo.getId());
        DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        if (buildNoVo == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        String status = verInfo.getString("status");
        DeployVersionBuildNoVo updateBuildNo = new DeployVersionBuildNoVo();
        updateBuildNo.setVersionId(versionVo.getId());
        updateBuildNo.setRunnerMapId(runnerId);
        updateBuildNo.setRunnerGroup(runnerGroup);
        updateBuildNo.setBuildNo(buildNo);
        updateBuildNo.setEndRev(verInfo.getString("endRev"));
        updateBuildNo.setStatus(status);
        deployVersionMapper.updateDeployVersionBuildNoByVersionIdAndBuildNo(updateBuildNo);

        DeployVersionVo updateVo = verInfo.toJavaObject(DeployVersionVo.class);
        updateVo.setId(versionVo.getId());
        updateVo.setRunnerMapId(runnerId);
        updateVo.setRunnerGroup(runnerGroup);
        if (BuildNoStatus.COMPILED.getValue().equals(status)) {
            updateVo.setIsCompiled(1);
        } else if (BuildNoStatus.COMPILE_FAILED.getValue().equals(status)) {
            updateVo.setIsCompiled(0);
        }
        deployVersionMapper.updateDeployVersionInfoById(updateVo);
        return null;
    }

}
