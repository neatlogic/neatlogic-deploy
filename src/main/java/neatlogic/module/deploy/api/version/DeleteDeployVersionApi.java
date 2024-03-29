package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployEnvVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployInstanceVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
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
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

    @Override
    public String getName() {
        return "删除发布版本";
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
            @Param(name = "id", desc = "版本id", type = ApiParamType.LONG),
            @Param(name = "sysId", desc = "应用ID", type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", type = ApiParamType.STRING),
    })
    @Description(desc = "删除发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
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
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
            if (versionVo != null) {
                deployVersionMapper.getDeployVersionLockById(versionVo.getId());
                versionId = versionVo.getId();
            }
        }
        if (versionId != null) {
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
        return null;
    }
}
