package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.CiJobType;
import codedriver.framework.deploy.constvalue.CiTriggerType;
import codedriver.framework.deploy.constvalue.RepoEvent;
import codedriver.framework.deploy.constvalue.RepoType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.exception.DeployCiIsRepeatException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployCiApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getName() {
        return "保存持续集成配置";
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
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "名称", rule = RegexUtils.NAME, maxLength = 50, type = ApiParamType.REGEX),
            @Param(name = "appSystemId", desc = "应用ID", type = ApiParamType.LONG),
            @Param(name = "appModuleId", desc = "模块ID", type = ApiParamType.LONG),
            @Param(name = "repoType", member = RepoType.class, desc = "仓库类型", type = ApiParamType.ENUM),
            @Param(name = "repoServerAddress", maxLength = 50, desc = "仓库服务器地址", type = ApiParamType.STRING),
            @Param(name = "repoName", maxLength = 50, rule = RegexUtils.NAME, desc = "仓库名称", type = ApiParamType.REGEX),
            @Param(name = "branches", desc = "分支", type = ApiParamType.JSONARRAY),
            @Param(name = "event", member = RepoEvent.class, desc = "事件", type = ApiParamType.ENUM),
            @Param(name = "action", member = CiJobType.class, desc = "动作类型", type = ApiParamType.ENUM),
            @Param(name = "triggerType", member = CiTriggerType.class, desc = "触发类型", type = ApiParamType.ENUM),
            @Param(name = "triggerTime", desc = "触发时间", type = ApiParamType.STRING),
            @Param(name = "versionRule", desc = "版本号规则", type = ApiParamType.JSONOBJECT),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT),
    })
    @Description(desc = "保存持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiVo deployCiVo = paramObj.toJavaObject(DeployCiVo.class);
        if (deployCiMapper.checkDeployCiIsRepeat(deployCiVo) > 0) {
            throw new DeployCiIsRepeatException(deployCiVo.getName());
        }
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployCiVo.getAppSystemId()) == null) {
            throw new CiEntityNotFoundException(deployCiVo.getAppSystemId());
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployCiVo.getAppModuleId()) == null) {
            throw new CiEntityNotFoundException(deployCiVo.getAppModuleId());
        }
        deployCiMapper.insertDeployCi(deployCiVo);
        return null;
    }

    public IValid version() {
        return value -> {
            DeployCiVo deployCiVo = value.toJavaObject(DeployCiVo.class);
            if(deployCiMapper.checkDeployCiIsRepeat(deployCiVo) > 0){
                return new FieldValidResultVo(new DeployCiIsRepeatException(deployCiVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
