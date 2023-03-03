package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployCiRepoType;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.dto.runner.RunnerVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployCiService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class DeleteDeployCiApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(DeleteDeployCiApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployCiService deployCiService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "删除持续集成配置";
    }

    @Override
    public String getToken() {
        return "deploy/ci/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true),
    })
    @Description(desc = "删除持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployCiVo ci = deployCiMapper.getDeployCiById(id);
        if (ci != null) {
            deployAppAuthorityService.checkOperationAuth(ci.getAppSystemId(), DeployAppConfigAction.EDIT);
            if (DeployCiRepoType.GITLAB.getValue().equals(ci.getRepoType())) {
                ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                CiEntityVo system = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(ci.getAppSystemId());
                if (system == null) {
                    throw new CiEntityNotFoundException(ci.getAppSystemId());
                }
                CiEntityVo module = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(ci.getAppModuleId());
                if (module == null) {
                    throw new CiEntityNotFoundException(ci.getAppModuleId());
                }
                RunnerVo runnerVo = deployCiService.getRandomRunnerBySystemIdAndModuleId(system, module);
                deployCiService.deleteGitlabWebHook(ci, runnerVo.getUrl());
            }
        }
        deployCiMapper.deleteDeployCiById(id);
        deployCiMapper.deleteDeployCiAuditByCiId(id);
        return null;
    }

}
