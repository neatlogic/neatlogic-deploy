package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.constvalue.DeployFromType;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileHasBeenReferredException;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployProfileMapper;
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
        return "删除发布工具profile";
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
            @Param(name = "id", desc = "profile id", isRequired = true, type = ApiParamType.LONG)
    })
    @Description(desc = "发布工具profile删除接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }
        //查询是否被引用(产品确认：无需判断所属系统和关联工具，只需要考虑是否被系统、模块、环境使用)
        if (DependencyManager.getDependencyCount(DeployFromType.DEPLOY_PROFILE_CIENTITY, id) > 0) {
            throw new DeployProfileHasBeenReferredException(profileVo.getName());
        }
        deployProfileMapper.deleteProfileById(id);
        deployProfileMapper.deleteProfileOperationByProfileId(id);
        return null;
    }
}
