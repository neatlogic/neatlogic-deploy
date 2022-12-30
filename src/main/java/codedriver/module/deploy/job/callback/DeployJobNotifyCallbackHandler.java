/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.job.callback;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.dto.NotifyPolicyConfigVo;
import codedriver.framework.notify.dto.NotifyPolicyVo;
import codedriver.framework.transaction.util.TransactionUtil;
import codedriver.framework.util.NotifyPolicyUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.handler.DeployJobMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
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
        if (trigger != null) {
            DeployJobVo jobInfo = deployJobMapper.getDeployJobInfoByJobId(jobVo.getId());
            if (jobInfo != null) {
                Long appSystemId = jobInfo.getAppSystemId();
                if (appSystemId != null) {
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
                    Long notifyPolicyId = deployAppConfigMapper.getAppSystemNotifyPolicyIdByAppSystemId(appSystemId);
                    if (notifyPolicyId != null) {
                        NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyById(notifyPolicyId);
                        if (notifyPolicyVo != null) {
                            NotifyPolicyConfigVo policyConfig = notifyPolicyVo.getConfig();
                            if (policyConfig != null) {
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
                    }
                }
            }
        }
    }
}
