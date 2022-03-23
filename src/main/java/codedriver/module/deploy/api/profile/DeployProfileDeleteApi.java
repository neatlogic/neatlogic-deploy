package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
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
 * @date 2022/3/18 10:08 上午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeployProfileDeleteApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Override
    public String getName() {
        return "保存自动化工具profile";
    }

    @Override
    public String getToken() {
        return "deploy/profile/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "profile id", type = ApiParamType.LONG)
    })
    @Description(desc = "自动化工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (deployProfileMapper.checkProfileIsExists(id) == 0) {
            throw new DeployProfileIsNotFoundException(id);
        }
        //查询是否被引用

//        if (DependencyManager.getDependencyCount(AutoexecFromType.AUTOEXEC_PROFILE_TOOL_AND_SCRIPT, id) > 0) {
//            throw new AutoexecProfileHasBeenReferredException(profileVo.getName());
//        }
        deployProfileMapper.deleteProfileById(id);
        deployProfileMapper.deleteProfileOperateByProfileId(id);
        return null;
    }
}
