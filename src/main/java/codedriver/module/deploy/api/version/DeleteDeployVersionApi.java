package codedriver.module.deploy.api.version;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
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

import javax.annotation.Resource;

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
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "isLock", desc = "是否封版(0：解版，1：封版)", isRequired = true, type = ApiParamType.INTEGER)
    })
    @Description(desc = "删除发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long versionId = paramObj.getLong("id");
        DeployVersionVo deployVersionVo = deployVersionMapper.getDeployVersionById(versionId);
        if (deployVersionVo == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        deployVersionMapper.deleteDeployVersionById(versionId);
        return null;
    }
}
