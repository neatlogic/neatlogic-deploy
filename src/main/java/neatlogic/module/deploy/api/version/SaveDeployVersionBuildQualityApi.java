package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildQualityVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployVersionBuildQualityApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.savedeployversionbuildqualityapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/build/quality/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "nmdav.savedeployversionbuildqualityapi.input.param.desc.sysid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "nmdav.savedeployversionbuildqualityapi.input.param.desc.moduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "nmdav.savedeployversionbuildqualityapi.input.param.desc.version", isRequired = true, type = ApiParamType.STRING),
            @Param(explode = DeployVersionBuildQualityVo.class, desc = "nmdav.savedeployversionbuildqualityapi.input.param.desc.deployversionbuildqualityvo", type = ApiParamType.JSONOBJECT),
    })
    @Output({
    })
    @Description(desc = "nmdav.savedeployversionbuildqualityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        DeployVersionBuildQualityVo qualityVo = paramObj.toJavaObject(DeployVersionBuildQualityVo.class);
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        qualityVo.setVersionId(versionVo.getId());
        deployVersionMapper.insertDeployVersionBuildQuality(qualityVo);
        return null;
    }
}
