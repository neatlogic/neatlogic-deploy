/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
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

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

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
        Long appModuleId = 0L;
        if (jsonObj.containsKey("appModuleId")) {
            appModuleId = jsonObj.getLong("appModuleId");
        }

        //查询系统名称
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemCiEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntityVo == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        result.put("appSystemName", appSystemCiEntityVo.getName());

        //判断是否有配流水线
        List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
        if (CollectionUtils.isEmpty(appConfigVoList)) {
            throw new DeployAppConfigNotFoundException(appSystemCiEntityVo);
        }
        //场景

        DeployPipelineConfigVo pipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(jsonObj.toJavaObject(DeployAppConfigVo.class));
        if (pipelineConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appSystemCiEntityVo);
        }
        result.put("scenarioList", pipelineConfigVo.getScenarioList());
        result.put("defaultScenarioId", pipelineConfigVo.getDefaultScenarioId());

        //环境 根据appSystemId、appModuleId 获取 envList
        //模块 根据appSystemId、appModuleId 获取 appModuleList
        List<Long> appModuleIdList = new ArrayList<>();
        List<DeployAppEnvironmentVo> envList = new ArrayList<>();
        List<ResourceVo> appModuleList = new ArrayList<>();
        if (appModuleId != 0L) {
            appModuleIdList.add(appModuleId);
        } else {
            TenantContext.get().switchDataDatabase();
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            appModuleIdList.addAll(resourceCrossoverMapper.getAppSystemModuleIdListByAppSystemId(appSystemId));
            TenantContext.get().switchDefaultDatabase();
        }
        if (CollectionUtils.isNotEmpty(appModuleIdList)) {
            envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList, TenantContext.get().getDataDbName());
            TenantContext.get().switchDataDatabase();
            appModuleList = resourceCenterMapper.getAppModuleListByIdListSimple(appModuleIdList);
            TenantContext.get().switchDefaultDatabase();
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
