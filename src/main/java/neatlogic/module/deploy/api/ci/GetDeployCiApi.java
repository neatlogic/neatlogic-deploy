package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployCiApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getName() {
        return "nmdac.getdeployciapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/ci/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "nmdac.getdeployciapi.input.param.desc.id", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "nmdac.getdeployciapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiVo ciVo = deployCiMapper.getDeployCiById(paramObj.getLong("id"));
        if (MapUtils.isNotEmpty(ciVo.getConfig())) {
            String parallelPolicy = ciVo.getConfig().getString("parallelPolicy");
            Integer roundCount = ciVo.getConfig().getInteger("roundCount");
            if (StringUtils.isNotBlank(parallelPolicy) && roundCount != null) {
                ciVo.getConfig().put("parallelPolicy", AutoexecParallelPolicy.ROUND_COUNT.getValue());
            }
        }
        return ciVo;
    }

}
