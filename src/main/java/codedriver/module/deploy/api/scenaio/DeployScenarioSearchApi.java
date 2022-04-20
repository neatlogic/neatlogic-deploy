package codedriver.module.deploy.api.scenaio;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployFromType;
import codedriver.framework.deploy.dto.scenario.DeployScenarioVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployScenarioMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/4/15 3:04 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployScenarioSearchApi extends PrivateApiComponentBase {

    @Resource
    DeployScenarioMapper deployScenarioMapper;

    @Override
    public String getName() {
        return "查询发布场景列表";
    }

    @Override
    public String getToken() {
        return "deploy/scenario/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Description(desc = "查询发布场景列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployScenarioVo paramScenarioVo = paramObj.toJavaObject(DeployScenarioVo.class);
        List<DeployScenarioVo> returnScenarioList = new ArrayList<>();
        int ScenarioCount = deployScenarioMapper.getScenarioCount(paramScenarioVo);
        if (ScenarioCount > 0) {
            paramScenarioVo.setRowNum(ScenarioCount);
            returnScenarioList = deployScenarioMapper.searchScenario(paramScenarioVo);
            Map<Object, Integer> ciEntityReferredCountMap = DependencyManager.getBatchDependencyCount(DeployFromType.DEPLOY_SCENARIO_CIENTITY, returnScenarioList.stream().map(DeployScenarioVo::getId).collect(Collectors.toList()));
            if (!ciEntityReferredCountMap.isEmpty()) {
                for (DeployScenarioVo scenarioVo : returnScenarioList) {
                    scenarioVo.setCiEntityReferredCount(ciEntityReferredCountMap.get(scenarioVo.getId()));
                }
            }
        }
        return TableResultUtil.getResult(returnScenarioList, paramScenarioVo);
    }
}
