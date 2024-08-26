/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.deploy.api.notify.test;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.NOTIFY_POLICY_MODIFY;
import neatlogic.framework.autoexec.constvalue.JobUserType;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.notify.dto.NotifyReceiverVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.handler.DeployJobMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = NOTIFY_POLICY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestNotifyPolicyApi extends PrivateApiComponentBase {

    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private NotifyMapper notifyMapper;

    private final Logger logger = LoggerFactory.getLogger(TestNotifyPolicyApi.class);

    @Override
    public String getName() {
        return "测试通知策略";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业ID"),
            @Param(name = "jobStatus", type = ApiParamType.STRING, isRequired = true, desc = "作业状态"),
            @Param(name = "notifyPolicyId", type = ApiParamType.STRING, isRequired = true, desc = "通知策略ID")
    })
    @Output({
    })
    @Description(desc = "测试通知策略")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String jobStatus = paramObj.getString("jobStatus");
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTriggerByStatus(jobStatus);
        if (trigger == null) {
            return null;
        }
        DeployJobVo jobInfo = deployJobMapper.getDeployJobInfoByJobId(jobId);
        if (jobInfo == null) {
            return null;
        }
        Long appSystemId = jobInfo.getAppSystemId();
        if (appSystemId == null) {
            return null;
        }
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
        if (appSystemVo != null) {
            jobInfo.setAppSystemName(appSystemVo.getName());
            jobInfo.setAppSystemAbbrName(appSystemVo.getAbbrName());
        }
        Long appModuleId = jobInfo.getAppModuleId();
        if (appModuleId != null) {
            AppModuleVo appModuleVo = iAppSystemMapper.getAppModuleById(appModuleId);
            if (appModuleVo != null) {
                jobInfo.setAppModuleName(appModuleVo.getName());
                jobInfo.setAppModuleAbbrName(appModuleVo.getAbbrName());
            }
        }
//        String configStr = deployAppConfigMapper.getAppSystemNotifyPolicyConfigByAppSystemId(appSystemId);
//        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.parseObject(configStr, InvokeNotifyPolicyConfigVo.class);
//        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
//        invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
//        if (invokeNotifyPolicyConfigVo == null) {
//            return null;
//        }
        // 触发点被排除，不用发送邮件
//        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
//        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(trigger.getTrigger())) {
//            return null;
//        }
//        Long notifyPolicyId = invokeNotifyPolicyConfigVo.getPolicyId();
//        if (notifyPolicyId == null) {
//            return null;
//        }
        Long notifyPolicyId = paramObj.getLong("notifyPolicyId");
        NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyById(notifyPolicyId);
        if (notifyPolicyVo == null || notifyPolicyVo.getConfig() == null) {
            return null;
        }
        try {
            Map<String, List<NotifyReceiverVo>> receiverMap = new HashMap<>();
            if (!Objects.equals(jobInfo.getExecUser(), SystemUser.SYSTEM.getUserUuid())) {
                receiverMap.computeIfAbsent(JobUserType.EXEC_USER.getValue(), k -> new ArrayList<>())
                        .add(new NotifyReceiverVo(GroupSearch.USER.getValue(), jobInfo.getExecUser()));
            }
            String notifyAuditMessage = jobInfo.getId() + "-" + jobInfo.getName();
            NotifyPolicyUtil.execute(notifyPolicyVo.getHandler(), trigger, DeployJobMessageHandler.class
                    , notifyPolicyVo, null, null, receiverMap
                    , jobInfo, null, notifyAuditMessage);
        } catch (Exception ex) {
            logger.error("发布作业：" + jobInfo.getId() + "-" + jobInfo.getName() + "通知失败");
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/notify/policy/test";
    }
}
