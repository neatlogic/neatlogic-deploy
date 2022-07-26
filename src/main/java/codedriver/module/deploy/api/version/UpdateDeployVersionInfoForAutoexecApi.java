package codedriver.module.deploy.api.version;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionBuildNoNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
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
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersionLock(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        DeployVersionVo updateVo = verInfo.toJavaObject(DeployVersionVo.class);
        updateVo.setId(versionVo.getId());
        updateVo.setRunnerMapId(runnerId);
        updateVo.setRunnerGroup(runnerGroup);
        deployVersionMapper.updateDeployVersionInfoById(updateVo);
        DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        if (buildNoVo == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        DeployVersionBuildNoVo updateBuildNo = new DeployVersionBuildNoVo();
        updateBuildNo.setVersionId(versionVo.getId());
        updateBuildNo.setRunnerMapId(runnerId);
        updateBuildNo.setRunnerGroup(runnerGroup);
        updateBuildNo.setBuildNo(buildNo);
        updateBuildNo.setEndRev(verInfo.getString("endRev"));
        updateBuildNo.setStatus(verInfo.getString("status"));
        deployVersionMapper.updateDeployVersionBuildNoByVersionIdAndBuildNo(updateBuildNo);
        return null;
    }

}
