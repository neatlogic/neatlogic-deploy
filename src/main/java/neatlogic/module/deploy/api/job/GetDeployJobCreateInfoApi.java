/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployJobCreateInfoApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaj.getdeployjobcreateinfoapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid")
    })
    @Output({
    })
    @Description(desc = "nmdaj.getdeployjobcreateinfoapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId");
        if (appModuleId == null) {
            appModuleId = 0L;
        }
        //查询系统名称、简称
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
        if (appSystemVo == null) {
            throw new AppSystemNotFoundEditTargetException(appSystemId);
        }
        result.put("appSystemName", appSystemVo.getName());
        result.put("appSystemAbbrName", appSystemVo.getAbbrName());

        //判断是否有配流水线
        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isEmpty(appConfigVoList)) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        //场景
        DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(appSystemId)
                .withAppModuleId(appModuleId)
                .getConfig();
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemId);
        }
        result.put("scenarioList", pipelineConfigVo.getScenarioList());
        result.put("defaultScenarioId", pipelineConfigVo.getDefaultScenarioId());

        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        //环境 根据appSystemId、appModuleId 获取 envList
        //模块 根据appSystemId、appModuleId 获取 appModuleList
        List<Long> appModuleIdList = new ArrayList<>();
        List<DeployAppEnvironmentVo> envList = new ArrayList<>();
        List<ResourceVo> appModuleList = new ArrayList<>();
        if (appModuleId != 0L) {
            appModuleIdList.add(appModuleId);
        } else {
            appModuleIdList.addAll(resourceCrossoverMapper.getAppSystemModuleIdListByAppSystemId(appSystemId));
        }
        if (CollectionUtils.isNotEmpty(appModuleIdList)) {
            envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
            appModuleList = resourceCrossoverMapper.getAppModuleListByIdListSimple(appModuleIdList, true);
        }
        result.put("envList", envList);
        result.put("appModuleList", appModuleList);

        // 找出当前用户在该应用系统中拥护授权的场景id列表和环境id列表
        AutoexecCombopScenarioVo firstEnableScenario = null;
        AutoexecCombopScenarioVo defaultScenario = null;
        boolean defaultScenarioIdIsEnable = false;
        List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();
        List<Long> scenarioIdList = scenarioList.stream().map(AutoexecCombopScenarioVo::getScenarioId).collect(Collectors.toList());
        List<Long> envIdList = envList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());
        Set<String> actionList = DeployAppAuthChecker.builder(appSystemId).addScenarioActionList(scenarioIdList).addEnvActionList(envIdList).check();
        for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
            if (actionList.contains(scenarioVo.getScenarioId().toString())) {
                scenarioVo.setIsEnable(true);
                if (firstEnableScenario == null) {
                    firstEnableScenario = scenarioVo;
                }
                if (!defaultScenarioIdIsEnable && Objects.equals(scenarioVo.getScenarioId(), pipelineConfigVo.getDefaultScenarioId())) {
                    defaultScenarioIdIsEnable = true;
                    defaultScenario = scenarioVo;
                }
            } else {
                scenarioVo.setIsEnable(false);
            }
        }
        if (defaultScenarioIdIsEnable) {
            result.put("defaultSelectScenario", defaultScenario);
        } else {
            result.put("defaultSelectScenario", firstEnableScenario);
        }
        Long firstEnableEnvId = null;
        for (DeployAppEnvironmentVo environmentVo : envList) {
            if (actionList.contains(environmentVo.getId().toString())) {
                environmentVo.setIsEnable(true);
                if (firstEnableEnvId == null) {
                    firstEnableEnvId = environmentVo.getId();
                    result.put("defaultSelectEnv", environmentVo);
                }
            } else {
                environmentVo.setIsEnable(false);
            }
        }
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create/info/get";
    }
}
