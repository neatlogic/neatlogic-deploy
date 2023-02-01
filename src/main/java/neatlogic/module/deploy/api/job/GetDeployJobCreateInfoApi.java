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

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
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
        return "获取创建发布作业初始化信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id")
    })
    @Output({
    })
    @Description(desc = "获取创建发布作业初始化信息接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId") == null ? 0L : jsonObj.getLong("appModuleId");

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
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create/info/get";
    }
}
