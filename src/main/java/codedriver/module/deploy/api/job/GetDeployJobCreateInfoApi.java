/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployJobCreateInfoApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
        //场景
        DeployAppConfigVo appConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId, 0L));
        DeployPipelineConfigVo pipelineConfigVo = appConfigVo.getConfig();
        /*补充当前场景是否有BUILD分类的工具，前端需要根据此标识调用 不同的选择版本下拉接口*/
        List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();

        //1、找出当前组合工具的所有包含BUILD分类的工具的阶段
        List<DeployPipelinePhaseVo> combopPhaseList = pipelineConfigVo.getCombopPhaseList();
        List<String> combopPhaseListHasBuildTypeTool = new ArrayList<>();
        for (DeployPipelinePhaseVo pipelinePhaseVo : combopPhaseList) {
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = pipelinePhaseVo.getConfig().getPhaseOperationList();
            for (AutoexecCombopPhaseOperationVo operationVo : phaseOperationList) {
                if (StringUtils.equals(ToolType.TOOL.getValue(), operationVo.getOperationType()) && StringUtils.equals(operationVo.getTypeName(), "BUILD")) {
                    combopPhaseListHasBuildTypeTool.add(pipelinePhaseVo.getName());
                }
            }
        }

        //2、查询场景的阶段列表是否有BUILD分类的工具
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
                if (CollectionUtils.isNotEmpty(scenarioVo.getCombopPhaseNameList()) && Collections.disjoint(combopPhaseListHasBuildTypeTool, scenarioVo.getCombopPhaseNameList())) {
                    scenarioVo.setIsHasBuildTypeTool(1);
                }
            }
        }
        result.put("scenarioList", scenarioList);

        //环境 根据appSystemId、appModuleId 获取 envList
        //模块 根据appSystemId、appModuleId 获取 appModuleList
        List<Long> appModuleIdList = new ArrayList<>();
        List<DeployAppEnvironmentVo> envList = new ArrayList<>();
        List<CiEntityVo> appModuleList = new ArrayList<>();
        if (appModuleId != 0L) {
            appModuleIdList.add(appModuleId);
        } else {
            appModuleIdList.addAll(resourceCenterMapper.getAppSystemModuleIdListByAppSystemId(appSystemId, TenantContext.get().getDataDbName()));
        }
        if (CollectionUtils.isNotEmpty(appModuleIdList)) {
            envList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList, TenantContext.get().getDataDbName());
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            appModuleList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(appModuleIdList);
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
