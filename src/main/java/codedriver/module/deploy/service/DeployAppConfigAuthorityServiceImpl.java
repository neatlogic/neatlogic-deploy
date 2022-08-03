package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

@Service
public class DeployAppConfigAuthorityServiceImpl implements DeployAppConfigAuthorityService {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public JSONObject getAuthorityListBySystemId(Long appSystemId) {
        JSONObject returnObj = new JSONObject();
        returnObj.put("operationAuthList", DeployAppConfigAction.getValueTextList());
        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        returnObj.put("scenarioAuthList", pipelineConfigVo.getScenarioList());

        returnObj.put("envAuthList", deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, new ArrayList<>(), TenantContext.get().getDataDbName()));

        return returnObj;
    }
}
