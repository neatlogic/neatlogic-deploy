/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private PipelineMapper pipelineMapper;

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
        int rowNum = deployScheduleMapper.getScheduleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                String schemaName = TenantContext.get().getDataDbName();
                IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
                tbodyList = deployScheduleMapper.getScheduleList(searchVo);
                for (DeployScheduleVo scheduleVo : tbodyList) {
                    String type = scheduleVo.getType();
                    if (type.equals(ScheduleType.GENERAL.getValue())) {
                        AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId(), schemaName);
                        if (appSystemVo != null) {
                            scheduleVo.setAppSystemName(appSystemVo.getName());
                            scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
                        }
                        AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(scheduleVo.getAppModuleId(), schemaName);
                        if (appModuleVo != null) {
                            scheduleVo.setAppModuleName(appModuleVo.getName());
                            scheduleVo.setAppModuleAbbrName(appModuleVo.getAbbrName());
                        }
                    } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
                        String pipelineType = scheduleVo.getPipelineType();
                        if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                            // TODO 应用流水线功能还没实现
//                            AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(scheduleVo.getAppSystemId(), schemaName);
//                            if (appSystemVo != null) {
//                                scheduleVo.setAppSystemName(appSystemVo.getName());
//                                scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
//                            }
                        } else if (pipelineType.equals(PipelineType.GLOBAL.getValue())) {
                            String name = pipelineMapper.getPipelineNameById(scheduleVo.getPipelineId());
                            if (StringUtils.isNotBlank(name)) {
                                scheduleVo.setPipelineName(name);
                            }
                        }
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}
