/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.deploy.api.schedule;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.constvalue.ScheduleType;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleSearchVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
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
        DeployScheduleSearchVo searchVo = JSONObject.toJavaObject(paramObj, DeployScheduleSearchVo.class);
        if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
            searchVo.setIsHasAllAuthority(1);
        } else {
            searchVo.setIsHasAllAuthority(0);
            List<String> authorityActionList = new ArrayList<>();
            authorityActionList.add(DeployAppConfigAction.VIEW.getValue());
            searchVo.setAuthorityActionList(authorityActionList);
            searchVo.setAuthUuidList(UserContext.get().getUuidList());
        }
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
