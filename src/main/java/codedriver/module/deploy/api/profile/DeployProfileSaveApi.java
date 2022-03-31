package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import codedriver.framework.deploy.exception.profile.DeployProfileNameRepeatsException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployProfileMapper;
import codedriver.module.deploy.service.DeployProfileService;
import com.alibaba.fastjson.JSON;
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
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployProfileSaveApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    DeployProfileService deployProfileService;

    @Override
    public String getName() {
        return "保存发布工具profile";
    }

    @Override
    public String getToken() {
        return "deploy/profile/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "profile id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "profile 名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "fromSystemId", type = ApiParamType.LONG, desc = "所属系统id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "工具参数"),
            @Param(name = "autoexecOperationVoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "发布工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramProfileId = paramObj.getLong("id");
        DeployProfileVo profileVo = JSON.toJavaObject(paramObj, DeployProfileVo.class);
        if (paramProfileId != null && deployProfileMapper.checkProfileIsExists(paramProfileId) == 0) {
            throw new DeployProfileIsNotFoundException(paramProfileId);
        }

        //删除profile和tool、script的关系
        deployProfileMapper.deleteProfileOperationByProfileId(paramProfileId);
        //保存profile和tool、script的关系
        deployProfileService.saveProfileOperation(profileVo.getId(), profileVo.getAutoexecOperationVoList());

        if (paramProfileId != null) {
            deployProfileMapper.updateProfile(profileVo);
        } else {
            deployProfileMapper.insertProfile(profileVo);
        }
        return null;
    }

    public IValid name() {
        return value -> {
            DeployProfileVo vo = JSON.toJavaObject(value, DeployProfileVo.class);
            if (deployProfileMapper.checkProfileNameIsRepeats(vo) > 0) {
                return new FieldValidResultVo(new DeployProfileNameRepeatsException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
