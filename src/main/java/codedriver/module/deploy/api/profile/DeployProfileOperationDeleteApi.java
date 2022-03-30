package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/30 5:24 下午
 */
@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeployProfileOperationDeleteApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Override
    public String getName() {
        return "删除profile和脚本的关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID")
    })
    @Override
    @Description(desc = "删除profile和脚本的关系接口")
    public Object myDoService(JSONObject paramObj) throws Exception {
        deployProfileMapper.deleteProfileOperationByOperationId(paramObj.getLong("id"));
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/profile/operation/delete";
    }
}
