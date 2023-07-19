/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.job;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigActionType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityActionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid")
    })
    @Output({
    })
    @Description(desc = "nmdaj.getdeployjobcreateinfoapi.getname")
    @ResubmitInterval(value = 2)
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
            throw new CiEntityNotFoundException(appSystemId);
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
        List<Long> hasAuthorityEnvIdList = new ArrayList<>();
        List<Long> hasAuthorityScenarioIdList = new ArrayList<>();
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            // 拥有DEPLOY_MODIFY授权的用户，拥有所有授权
            for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : pipelineConfigVo.getScenarioList()) {
                hasAuthorityScenarioIdList.add(autoexecCombopScenarioVo.getScenarioId());
            }
            for (DeployAppEnvironmentVo appEnvironmentVo : envList) {
                hasAuthorityEnvIdList.add(appEnvironmentVo.getId());
            }
        } else {
            // 查询出当前用户拥有的权限
            List<DeployAppConfigAuthorityActionVo> actionList = deployAppConfigMapper.getDeployAppAllAuthorityActionListByAppSystemIdAndAuthUuidList(appSystemId, UserContext.get().getUuidList());
            for (DeployAppConfigAuthorityActionVo actionVo : actionList) {
                if (Objects.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue())) {
                    if (Objects.equals(actionVo.getAction(), "all")) {
                        for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : pipelineConfigVo.getScenarioList()) {
                            if (!hasAuthorityScenarioIdList.contains(autoexecCombopScenarioVo.getScenarioId())) {
                                hasAuthorityScenarioIdList.add(autoexecCombopScenarioVo.getScenarioId());
                            }
                        }
                        break;
                    } else {
                        hasAuthorityScenarioIdList.add(Long.valueOf(actionVo.getAction()));
                    }
                }
            }
            for (DeployAppConfigAuthorityActionVo actionVo : actionList) {
                if (Objects.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue())) {
                    if (Objects.equals(actionVo.getAction(), "all")) {
                        for (DeployAppEnvironmentVo appEnvironmentVo : envList) {
                            if (!hasAuthorityEnvIdList.contains(appEnvironmentVo.getId())) {
                                hasAuthorityEnvIdList.add(appEnvironmentVo.getId());
                            }
                        }
                        break;
                    } else {
                        hasAuthorityEnvIdList.add(Long.valueOf(actionVo.getAction()));
                    }
                }
            }
        }
        result.put("hasAuthorityScenarioIdList", hasAuthorityScenarioIdList);
        result.put("hasAuthorityEnvIdList", hasAuthorityEnvIdList);
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create/info/get";
    }
}
