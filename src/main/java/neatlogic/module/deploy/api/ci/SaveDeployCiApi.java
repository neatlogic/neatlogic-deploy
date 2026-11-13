package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.*;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.deploy.service.DeployCiWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployCiApi extends PrivateApiComponentBase {

    @Resource
    private DeployCiMapper deployCiMapper;
    @Resource
    private DeployCiWriter writer;

    @Override
    public String getName() {
        return "nmdac.savedeployciapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/ci/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "common.id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "common.name", rule = RegexUtils.NAME, maxLength = 50, type = ApiParamType.REGEX, isRequired = true),
            @Param(name = "isActive", desc = "common.isactive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true),
            @Param(name = "appSystemId", desc = "term.cmdb.appsystemid", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "appModuleId", desc = "term.cmdb.moduleid", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "repoType", desc = "nmdac.savedeployciapi.input.param.desc.repotype", member = DeployCiRepoType.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "repoServerAddress", desc = "nmdac.savedeployciapi.input.param.desc.reposerveraddress", rule = RegexUtils.CONNECT_URL, type = ApiParamType.REGEX, maxLength = 100, isRequired = true),
            @Param(name = "repoName", desc = "nmdac.savedeployciapi.input.param.desc.reponame", rule = RegexUtils.NAME_WITH_SLASH, maxLength = 50, type = ApiParamType.REGEX, isRequired = true),
            @Param(name = "branchFilter", desc = "common.branch", type = ApiParamType.STRING),
            @Param(name = "event", desc = "common.event", member = DeployCiRepoEvent.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "action", desc = "common.action", member = DeployCiActionType.class, type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerType", member = DeployCiTriggerType.class, desc = "common.triggertype", type = ApiParamType.ENUM, isRequired = true),
            @Param(name = "triggerTime", desc = "common.triggertime", type = ApiParamType.STRING),
            @Param(name = "delayTime", desc = "common.delaytime", type = ApiParamType.INTEGER),
            @Param(name = "versionRule", desc = "nmdac.savedeployciapi.input.param.desc.versionrule", type = ApiParamType.JSONOBJECT),
            @Param(name = "config", desc = "common.config", type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "nmdac.savedeployciapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        writer.save(paramObj.toJavaObject(DeployCiVo.class), false);
        return null;
    }

    public IValid name() {
        return value -> {
            DeployCiVo deployCiVo = value.toJavaObject(DeployCiVo.class);
            if (deployCiMapper.checkDeployCiIsRepeat(deployCiVo) > 0) {
                return new FieldValidResultVo(new DeployCiIsRepeatException(deployCiVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
