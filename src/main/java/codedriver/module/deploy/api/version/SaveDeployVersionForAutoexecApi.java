package codedriver.module.deploy.api.version;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.BuildNoStatus;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
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
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployVersionForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "保存发布版本";
    }

    @Override
    public String getToken() {
        return "deploy/version/save/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "runnerId", desc = "runnerId", type = ApiParamType.LONG),
            @Param(name = "runnerGroup", desc = "runnerGroup", type = ApiParamType.JSONOBJECT),
            @Param(name = "jobId", desc = "作业id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "buildNo", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "verInfo", desc = "版本信息", isRequired = true, type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "保存发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long runnerId = paramObj.getLong("runnerId");
        JSONObject runnerGroup = paramObj.getJSONObject("runnerGroup");
        Long jobId = paramObj.getLong("jobId");
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        JSONObject verInfo = paramObj.getJSONObject("verInfo");
        String status = verInfo.getString("status");
        DeployVersionVo versionVo = verInfo.toJavaObject(DeployVersionVo.class);
        DeployVersionVo oldVersionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersionLock(new DeployVersionVo(version, sysId, moduleId));
        if (oldVersionVo == null) {
            versionVo.setAppSystemId(sysId);
            versionVo.setAppModuleId(moduleId);
            versionVo.setVersion(version);
            versionVo.setRunnerMapId(runnerId);
            versionVo.setRunnerGroup(runnerGroup);
            if (BuildNoStatus.COMPILED.getValue().equals(status)) {
                versionVo.setCompileSuccessCount(1);
            } else if (BuildNoStatus.COMPILE_FAILED.getValue().equals(status)) {
                versionVo.setCompileFailCount(1);
            }
            deployVersionMapper.insertDeployVersion(versionVo);
        } else {
            versionVo.setId(oldVersionVo.getId());
            versionVo.setRunnerMapId(runnerId);
            versionVo.setRunnerGroup(runnerGroup);
            if (BuildNoStatus.COMPILED.getValue().equals(status)) {
                versionVo.setIsCompiled(1);
            } else if (BuildNoStatus.COMPILE_FAILED.getValue().equals(status)) {
                versionVo.setIsCompiled(0);
            }
            deployVersionMapper.updateDeployVersionInfoById(versionVo);
        }
        DeployVersionBuildNoVo buildNoVo = new DeployVersionBuildNoVo(versionVo.getId(), buildNo, jobId, status, runnerId, runnerGroup, verInfo.getString("endRev"));
        deployVersionMapper.insertDeployVersionBuildNo(buildNoVo);
        return null;
    }

}
