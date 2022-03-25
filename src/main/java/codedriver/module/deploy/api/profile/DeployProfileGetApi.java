package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployProfileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployProfileGetApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    DeployProfileService deployProfileService;

    @Override
    public String getName() {
        return "获取自动化工具profile";
    }

    @Override
    public String getToken() {
        return "deploy/profile/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "profile id", type = ApiParamType.LONG)
    })
    @Output({
            @Param(explode = DeployProfileVo[].class, desc = "工具profile")
    })
    @Description(desc = "获取自动化工具profile接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }
        //获取profile关联的tool、script工具
        profileVo.setAutoexecToolAndScriptVoList(deployProfileService.getAutoexecToolAndScriptVoListByProfileId(id));
        //获取profile参数
        profileVo.setParamList(deployProfileService.getProfileParamById(id));
        return profileVo;
    }
}
