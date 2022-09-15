/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.trigger;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobStatusVo;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.deploy.dto.trigger.DeployJobTriggerConfigVo;
import codedriver.framework.deploy.dto.trigger.DeployJobTriggerVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployJobTriggerMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployJobTriggerApi extends PrivateApiComponentBase {
    @Resource
    DeployJobTriggerMapper triggerMapper;

    @Override
    public String getName() {
        return "查询发布作业触发器";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = DeployScheduleVo[].class, desc = "定时作业列表"),})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployJobTriggerVo deployJobTriggerVo = paramObj.toJavaObject(DeployJobTriggerVo.class);
        List<DeployJobTriggerVo> triggerVoList = new ArrayList<>();
        int count = triggerMapper.getTriggerCount(deployJobTriggerVo);
        if (count > 0) {
            deployJobTriggerVo.setRowNum(count);
            List<Long> triggerIdList = triggerMapper.getTriggerIdList(deployJobTriggerVo);
            triggerVoList = triggerMapper.getTriggerListByIdList(triggerIdList);
            //补充状态、源环境
            IResourceCrossoverMapper appSystemMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<AppEnvironmentVo> environmentVos = appSystemMapper.getAllAppEnv(TenantContext.get().getDataDbName());
            Map<Long, AppEnvironmentVo> envMap = environmentVos.stream().collect(Collectors.toMap(AppEnvironmentVo::getEnvId, e -> e));
            for (DeployJobTriggerVo trigger : triggerVoList) {
                DeployJobTriggerConfigVo config = trigger.getConfig();
                List<Long> envIdList = config.getEnvIdList();
                List<AppEnvironmentVo> envList = new ArrayList<>();
                for (Long envId : envIdList) {
                    envList.add(envMap.get(envId));
                }
                trigger.setEnvList(envList);
                List<String> statusList = config.getJobStatusList();
                List<AutoexecJobStatusVo> jobStatusVoList = new ArrayList<>();
                for (String status : statusList) {
                    jobStatusVoList.add(JobStatus.getStatus(status));
                }
                trigger.setJobStatusList(jobStatusVoList);
            }
            return TableResultUtil.getResult(triggerVoList, deployJobTriggerVo);
        }
        return triggerVoList;
    }

    @Override
    public String getToken() {
        return "/deploy/job/trigger/search";
    }
}
