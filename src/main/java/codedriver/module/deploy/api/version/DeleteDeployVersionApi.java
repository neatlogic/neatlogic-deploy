package codedriver.module.deploy.api.version;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/14 9:59 上午
 */
@Service
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

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
            @Param(name = "moduleId", desc = "应用系统id", type = ApiParamType.LONG),
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
        if (versionId != null) {
            deployVersionMapper.deleteDeployVersionById(versionId);
        } else {
            deployVersionMapper.deleteDeployVersionBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        }
        return null;
    }
}
