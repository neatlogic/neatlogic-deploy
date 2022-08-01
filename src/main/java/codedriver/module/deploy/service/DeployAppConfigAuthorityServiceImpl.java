package codedriver.module.deploy.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DeployAppConfigAuthorityServiceImpl implements DeployAppConfigAuthorityService {


    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Override
    public JSONArray getAuthorityListBySystemId(Long appSystemId) {
        JSONArray resultArray = DeployAppConfigAction.getValueTextList();
        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId));

        if(CollectionUtils.isNotEmpty(pipelineConfigVo.getScenarioList())){
            for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                JSONObject scenarioKeyValue = new JSONObject();
                scenarioKeyValue.put("value", scenarioVo.getScenarioName());
                scenarioKeyValue.put("text", scenarioVo.getScenarioName());
                resultArray.add(scenarioKeyValue);
            }
        }
        return resultArray;
    }
}
