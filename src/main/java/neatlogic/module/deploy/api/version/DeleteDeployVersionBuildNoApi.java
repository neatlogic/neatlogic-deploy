package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployVersionBuildNoApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "删除发布版本buildNo";
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
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "buildNo", desc = "buildNo", isRequired = true, type = ApiParamType.INTEGER),
    })
    @Description(desc = "删除发布版本buildNo")
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
