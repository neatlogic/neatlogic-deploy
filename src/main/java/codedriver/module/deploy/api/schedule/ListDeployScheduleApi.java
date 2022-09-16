/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.PIPELINE_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.auth.core.DeployAppAuthChecker;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private DeployPipelineMapper deployPipelineMapper;

    @Override
    public String getToken() {
        return "deploy/schedule/list";
    }

    @Override
    public String getName() {
        return "查询定时作业列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployScheduleVo[].class, desc = "定时作业列表"),
    })
    @Description(desc = "查询定时作业列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployScheduleVo searchVo = JSONObject.toJavaObject(paramObj, DeployScheduleVo.class);
        List<DeployScheduleVo> tbodyList = new ArrayList<>();
        String userUuid = UserContext.get().getUserUuid(true);
        int rowNum = deployScheduleMapper.getScheduleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
                tbodyList = deployScheduleMapper.getScheduleList(searchVo);
                List<Long> idList = tbodyList.stream().map(DeployScheduleVo::getId).collect(Collectors.toList());
                List<DeployScheduleVo> scheduleAuditCountList = deployScheduleMapper.getScheduleAuditCountListByIdList(idList);
                Map<Long, DeployScheduleVo> scheduleMap = scheduleAuditCountList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                Map<Long, AppSystemVo> appSystemMap = new HashMap<>();
                List<Long> appSystemIdList = tbodyList.stream().map(DeployScheduleVo::getAppSystemId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                    List<AppSystemVo> appSystemList = appSystemMapper.getAppSystemListByIdList(appSystemIdList);
                    appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                }
                Map<Long, AppModuleVo> appModuleMap = new HashMap<>();
                List<Long> appModuleIdList = tbodyList.stream().map(DeployScheduleVo::getAppModuleId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                    List<AppModuleVo> appModuleList = appSystemMapper.getAppModuleListByIdList(appModuleIdList);
                    appModuleMap = appModuleList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                }
                List<Long> pipelineIdList = tbodyList.stream().map(DeployScheduleVo::getPipelineId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(pipelineIdList)) {
                    pipelineIdList = deployPipelineMapper.checkHasAuthPipelineIdList(pipelineIdList, userUuid);
                }
                boolean hasPipelineModify = AuthActionChecker.check(PIPELINE_MODIFY.class);
                for (DeployScheduleVo scheduleVo : tbodyList) {
                    DeployScheduleVo scheduleAuditCount = scheduleMap.get(scheduleVo.getId());
                    if (scheduleAuditCount != null) {
                        scheduleVo.setExecCount(scheduleAuditCount.getExecCount());
                    }
                    String type = scheduleVo.getType();
                    if (type.equals(ScheduleType.GENERAL.getValue())) {
                        Long appSystemId = scheduleVo.getAppSystemId();
                        AppSystemVo appSystemVo = appSystemMap.get(appSystemId);
                        if (appSystemVo != null) {
                            scheduleVo.setAppSystemName(appSystemVo.getName());
                            scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
                        }
                        AppModuleVo appModuleVo = appModuleMap.get(scheduleVo.getAppModuleId());
                        if (appModuleVo != null) {
                            scheduleVo.setAppModuleName(appModuleVo.getName());
                            scheduleVo.setAppModuleAbbrName(appModuleVo.getAbbrName());
                        }
                        DeployScheduleConfigVo config = scheduleVo.getConfig();
                        Set<String> actionSet = DeployAppAuthChecker.builder(appSystemId)
                                .addEnvAction(config.getEnvId())
                                .addScenarioAction(config.getScenarioId())
                                .check();
                        if (actionSet.contains(config.getEnvId().toString()) && actionSet.contains(config.getScenarioId().toString())) {
                            scheduleVo.setEditable(1);
                            scheduleVo.setDeletable(1);
                        }
                    } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
                        String name = deployPipelineMapper.getPipelineNameById(scheduleVo.getPipelineId());
                        if (StringUtils.isNotBlank(name)) {
                            scheduleVo.setPipelineName(name);
                        }
                        String pipelineType = scheduleVo.getPipelineType();
                        if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                            Long appSystemId = scheduleVo.getAppSystemId();
                            AppSystemVo appSystemVo = appSystemMap.get(appSystemId);
                            if (appSystemVo != null) {
                                scheduleVo.setAppSystemName(appSystemVo.getName());
                                scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
                            }
                            if (pipelineIdList.contains(scheduleVo.getPipelineId())) {
                                scheduleVo.setEditable(1);
                                scheduleVo.setDeletable(1);
                            } else {
                                Set<String> actionSet = DeployAppAuthChecker.builder(appSystemId)
                                        .addOperationAction(DeployAppConfigAction.PIPELINE.getValue())
                                        .check();
                                if (actionSet.contains(DeployAppConfigAction.PIPELINE.getValue())) {
                                    scheduleVo.setEditable(1);
                                    scheduleVo.setDeletable(1);
                                }
                            }
                        } else if (pipelineType.equals(PipelineType.GLOBAL.getValue())) {
                            if (hasPipelineModify || pipelineIdList.contains(scheduleVo.getPipelineId())) {
                                scheduleVo.setEditable(1);
                                scheduleVo.setDeletable(1);
                            }
                        }
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}
