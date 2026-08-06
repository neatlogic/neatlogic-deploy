package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionInfoForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.getdeployversioninfoforautoexecapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/info/get/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "nmdav.getdeployversioninfoforautoexecapi.input.param.desc.buildno", isRequired = true, type = ApiParamType.INTEGER),
    })
    @Description(desc = "nmdav.getdeployversioninfoforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            return null;
        }
        DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        if (buildNoVo == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.put("version", version);
        result.put("buildNo", buildNo);
        result.put("repoType", versionVo.getRepoType());
        result.put("repo", versionVo.getRepo());
        result.put("trunk", versionVo.getTrunk());
        result.put("branch", versionVo.getBranch());
        result.put("tag", versionVo.getTag());
        result.put("tagsDir", versionVo.getTagsDir());
        result.put("isFreeze", versionVo.getIsFreeze());
        result.put("startRev", versionVo.getStartRev());
        result.put("endRev", buildNoVo.getEndRev());
        result.put("status", buildNoVo.getStatus());
        return result;
    }

}
