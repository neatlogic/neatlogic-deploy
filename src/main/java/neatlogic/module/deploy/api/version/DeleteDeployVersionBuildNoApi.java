package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployVersionBuildNoApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdav.deletedeployversionbuildnoapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/buildNo/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "nmdav.deletedeployversionbuildnoapi.input.param.desc.buildno", isRequired = true, type = ApiParamType.INTEGER),
    })
    @Description(desc = "nmdav.deletedeployversionbuildnoapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验版本&制品管理的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("sysId"), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);

        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        Integer buildNo = paramObj.getInteger("buildNo");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo != null) {
            deployVersionMapper.deleteDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), buildNo);
        }
        return null;
    }
}
