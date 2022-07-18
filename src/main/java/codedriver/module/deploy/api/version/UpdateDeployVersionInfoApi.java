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
public class UpdateDeployVersionInfoApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "更新发布版本配置";
    }

    @Override
    public String getToken() {
        return "deploy/version/info/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用系统id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "buildNo", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "verInfo", desc = "版本信息", isRequired = true, type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "更新发布版本配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        JSONObject verInfo = paramObj.getJSONObject("verInfo");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersionLock(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        DeployVersionVo updateVo = new DeployVersionVo();
        updateVo.setId(versionVo.getId());
        updateVo.setRepoType(verInfo.getString("repoType"));
        updateVo.setRepo(verInfo.getString("repo"));
        updateVo.setTrunk(verInfo.getString("trunk"));
        updateVo.setBranch(verInfo.getString("branch"));
        updateVo.setTag(verInfo.getString("tag"));
        updateVo.setTagsDir(verInfo.getString("tagsDir"));
        updateVo.setIsFreeze(verInfo.getInteger("isFreeze"));
        updateVo.setStartRev(verInfo.getString("startRev"));
        String endRev = verInfo.getString("endRev");
        updateVo.setEndRev(endRev);
        deployVersionMapper.updateDeployVersionInfoById(updateVo);
        DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        if (buildNoVo == null) {
            throw new DeployVersionBuildNoNotFoundException(versionVo.getVersion(), buildNo);
        }
        DeployVersionBuildNoVo updateBuildNo = new DeployVersionBuildNoVo();
        updateBuildNo.setVersionId(versionVo.getId());
        updateBuildNo.setBuildNo(buildNo);
        updateBuildNo.setEndRev(endRev);
        updateBuildNo.setStatus(verInfo.getString("status"));
        deployVersionMapper.updateDeployVersionBuildNoByVersionIdAndBuildNo(updateBuildNo);
        return null;
    }

}
