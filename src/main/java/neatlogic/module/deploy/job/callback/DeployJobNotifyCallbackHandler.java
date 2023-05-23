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
package neatlogic.module.deploy.job.callback;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.handler.DeployJobMessageHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/12/29 15:32
 */

@Component
public class DeployJobNotifyCallbackHandler extends AutoexecJobCallbackBase {

    private final static Logger logger = LoggerFactory.getLogger(DeployJobNotifyCallbackHandler.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getHandler() {
        return DeployJobNotifyCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTrigger(jobVo.getStatus());
        if (trigger != null) {
            AutoexecJobVo jobInfo;
            // 开启一个新事务来查询父事务提交前的作业状态，如果新事务查出来的状态与当前jobVo的状态不同，则表示该状态未通知过
            TransactionStatus tx = TransactionUtil.openNewTx();
            try {
                jobInfo = autoexecJobMapper.getJobInfo(jobVo.getId());
            } finally {
                if (tx != null) {
                    TransactionUtil.commitTx(tx);
                }
            }
            if (jobInfo != null && !Objects.equals(jobVo.getStatus(), jobInfo.getStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTrigger(jobVo.getStatus());
        if (trigger == null) {
            return;
        }
        DeployJobVo jobInfo = deployJobMapper.getDeployJobInfoByJobId(jobVo.getId());
        if (jobInfo == null) {
            return;
        }
        Long appSystemId = jobInfo.getAppSystemId();
        if (appSystemId == null) {
            return;
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
        String configStr = deployAppConfigMapper.getAppSystemNotifyPolicyConfigByAppSystemId(appSystemId);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.parseObject(configStr, InvokeNotifyPolicyConfigVo.class);
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
        if (invokeNotifyPolicyConfigVo == null) {
            return;
        }
        // 触发点被排除，不用发送邮件
        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(trigger.getTrigger())) {
            return;
        }
        Long notifyPolicyId = invokeNotifyPolicyConfigVo.getPolicyId();
        if (notifyPolicyId == null) {
            return;
        }
        NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyById(notifyPolicyId);
        if (notifyPolicyVo == null || notifyPolicyVo.getConfig() == null) {
            return;
        }
        try {
            String notifyAuditMessage = jobInfo.getId() + "-" + jobInfo.getName();
            NotifyPolicyUtil.execute(notifyPolicyVo.getHandler(), trigger, DeployJobMessageHandler.class
                    , notifyPolicyVo, null, null, null
                    , jobInfo, null, notifyAuditMessage);
        } catch (Exception ex) {
            logger.error("发布作业：" + jobInfo.getId() + "-" + jobInfo.getName() + "通知失败");
            logger.error(ex.getMessage(), ex);
        }
    }
}
