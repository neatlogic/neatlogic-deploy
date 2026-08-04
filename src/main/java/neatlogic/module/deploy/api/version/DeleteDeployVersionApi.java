package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployEnvVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployInstanceVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/14 9:59 上午
 */
@Service
@AuthUser(SystemUser.AUTOEXEC)
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdav.deletedeployversionapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "nmdav.deletedeployversionapi.input.param.desc.id", type = ApiParamType.LONG),
            @Param(name = "sysId", desc = "nmdav.deletedeployversionapi.input.param.desc.sysid", type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "nmdav.deletedeployversionapi.input.param.desc.moduleid", type = ApiParamType.LONG),
            @Param(name = "version", desc = "nmdav.deletedeployversionapi.input.param.desc.version", type = ApiParamType.STRING),
    })
    @Description(desc = "nmdav.deletedeployversionapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployVersionVo versionVo;
        Long versionId = paramObj.getLong("id");
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        if (versionId == null && (sysId == null || moduleId == null || version == null)) {
            List<String> paramList = new ArrayList<>();
            paramList.add("sysId");
            paramList.add("moduleId");
            paramList.add("version");
            throw new ParamNotExistsException(Collections.singletonList("id"), paramList);
        }
        if (versionId == null) {
             versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
             if(versionVo != null) {
                 versionId = versionVo.getId();
             }
        }

        if(versionId != null) {
            versionVo = deployVersionMapper.getDeployVersionLockById(versionId);
            if (versionVo != null) {
                deployAppAuthorityService.checkOperationAuth(versionVo.getAppSystemId(), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);
                deployVersionMapper.deleteDeployVersionById(versionId);
                deployVersionMapper.deleteDeployVersionBuildNoByVersionId(versionId);
                deployVersionMapper.deleteDeployVersionEnvByVersionId(versionId);
                deployEnvVersionMapper.deleteDeployEnvVersionByVersionId(versionId);
                deployEnvVersionMapper.deleteDeployEnvVersionAuditByVersionId(versionId);
                deployInstanceVersionMapper.deleteDeployInstanceVersionByVersionId(versionId);
                deployInstanceVersionMapper.deleteDeployInstanceVersionAuditByVersionId(versionId);
                deployVersionMapper.deleteDeployVersionDependencyByVersionId(versionId);
                deployVersionMapper.deleteDeployedInstanceByVersionId(versionId);
                deployVersionMapper.deleteDeployVersionBuildQualityByVersionId(versionId);
                deployVersionMapper.deleteDeployVersionUnitTestByVersionId(versionId);
            }
        }
        return null;
    }
}
