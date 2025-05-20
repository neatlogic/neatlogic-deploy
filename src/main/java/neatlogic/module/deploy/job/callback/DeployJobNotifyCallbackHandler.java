/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
package neatlogic.module.deploy.job.callback;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobUserType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.notify.dto.NotifyReceiverVo;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.handler.DeployJobMessageHandler;
import neatlogic.module.deploy.notify.handler.DeployJobNotifyPolicyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.*;

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
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
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
        boolean flag = false;
        StringBuilder notifyAuditMessageStringBuilder = new StringBuilder();
        try {
            DeployJobNotifyTriggerType notifyTriggerType = DeployJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
            if (notifyTriggerType != null) {
                notifyAuditMessageStringBuilder.append("触发点为 ").append(notifyTriggerType.getTrigger()).append("(").append(notifyTriggerType.getText()).append(")");
                DeployJobVo jobInfo = deployJobMapper.getDeployJobInfoByJobId(jobVo.getId());
                if (jobInfo != null) {
                    notifyAuditMessageStringBuilder
                            .append(" 作业")
                            .append(jobInfo.getName())
                            .append("(")
                            .append(jobVo.getId())
                            .append(")")
                            .append("的状态为")
                            .append(jobVo.getStatus());
                    notifyAuditMessageStringBuilder.append(" 执行用户").append(jobInfo.getExecUser());
                    Long appSystemId = jobInfo.getAppSystemId();
                    if (appSystemId != null) {
                        notifyAuditMessageStringBuilder.append(" 应用系统");
                        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
                        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
                        if (appSystemVo != null) {
                            jobInfo.setAppSystemName(appSystemVo.getName());
                            jobInfo.setAppSystemAbbrName(appSystemVo.getAbbrName());
                            notifyAuditMessageStringBuilder.append(appSystemVo.getName())
                                    .append("(").append(appSystemVo.getAbbrName()).append(")");
                        }
                        notifyAuditMessageStringBuilder.append("(").append(appSystemId).append(")");
                        Long appModuleId = jobInfo.getAppModuleId();
                        if (appModuleId != null) {
                            notifyAuditMessageStringBuilder.append(" 应用模块");
                            AppModuleVo appModuleVo = iAppSystemMapper.getAppModuleById(appModuleId);
                            if (appModuleVo != null) {
                                jobInfo.setAppModuleName(appModuleVo.getName());
                                jobInfo.setAppModuleAbbrName(appModuleVo.getAbbrName());
                                notifyAuditMessageStringBuilder.append(appModuleVo.getName())
                                        .append("(").append(appModuleVo.getAbbrName()).append(")");
                            }
                            notifyAuditMessageStringBuilder.append("(").append(appModuleId).append(")");
                        }
                        Long envId = jobInfo.getEnvId();
                        if (envId != null) {
                            notifyAuditMessageStringBuilder.append(" 环境");
                            if (StringUtils.isNotBlank(jobInfo.getEnvName())) {
                                notifyAuditMessageStringBuilder.append(jobInfo.getEnvName());
                            }
                            notifyAuditMessageStringBuilder.append("(").append(envId).append(")");
                        }
                        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = null;
                        String configStr = deployAppConfigMapper.getAppSystemNotifyPolicyConfigByAppSystemId(appSystemId);
                        if (StringUtils.isNotBlank(configStr)) {
                            invokeNotifyPolicyConfigVo = JSONObject.parseObject(configStr, InvokeNotifyPolicyConfigVo.class);
                        } else {
//                            notifyAuditMessageStringBuilder.append(" 在deploy_job_notify_policy表中找不到的该应用系统的通知策略数据").append("，无法触发通知");
                            invokeNotifyPolicyConfigVo = new InvokeNotifyPolicyConfigVo();
                            invokeNotifyPolicyConfigVo.setHandler(DeployJobNotifyPolicyHandler.class.getName());
                        }
                        // 触发点被排除，不用发送邮件
                        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
                        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(notifyTriggerType.getTrigger())) {
                            notifyAuditMessageStringBuilder.append(" 通知策略设置触发时机中排除了触发点").append(notifyTriggerType.getTrigger()).append("(").append(notifyTriggerType.getText()).append(")");
                        } else {
                            NotifyPolicyVo notifyPolicyVo = null;
                            if (invokeNotifyPolicyConfigVo.getIsCustom() == 1) {
                                notifyAuditMessageStringBuilder.append(" 设置通知策略ID为 ");
                                if (invokeNotifyPolicyConfigVo.getPolicyId() != null) {
                                    notifyAuditMessageStringBuilder.append(invokeNotifyPolicyConfigVo.getPolicyId());
                                    notifyPolicyVo = notifyMapper.getNotifyPolicyById(invokeNotifyPolicyConfigVo.getPolicyId());
                                    if (notifyPolicyVo == null) {
                                        notifyAuditMessageStringBuilder.append("，但是该通知策略不存在");
                                    } else {
                                        notifyAuditMessageStringBuilder.append("，找到默认通知策略").append(notifyPolicyVo.getName()).append("(").append(notifyPolicyVo.getId()).append(")");
                                    }
                                } else {
                                    notifyAuditMessageStringBuilder.append("null");
                                }
                            } else {
                                notifyAuditMessageStringBuilder.append(" 没有设置通知策略 ");
                                if (invokeNotifyPolicyConfigVo.getHandler() != null) {
                                    notifyAuditMessageStringBuilder.append(" 通过通知策略handler=").append(invokeNotifyPolicyConfigVo.getHandler());
                                    notifyPolicyVo = notifyMapper.getDefaultNotifyPolicyByHandler(invokeNotifyPolicyConfigVo.getHandler());
                                    if (notifyPolicyVo == null) {
                                        notifyAuditMessageStringBuilder.append(" ，找不到默认通知策略");
                                    } else {
                                        notifyAuditMessageStringBuilder.append(" ，找到默认通知策略 ").append(notifyPolicyVo.getName()).append("(").append(notifyPolicyVo.getId()).append(")");
                                    }
                                } else {
                                    notifyAuditMessageStringBuilder.append(" 由于通知策略handler为null，无法找到默认通知策略");
                                }
                            }
                            if (notifyPolicyVo != null) {
                                if (notifyPolicyVo.getConfig() != null) {
                                    Map<String, List<NotifyReceiverVo>> receiverMap = new HashMap<>();
                                    if (!Objects.equals(jobInfo.getExecUser(), SystemUser.SYSTEM.getUserUuid())) {
                                        receiverMap.computeIfAbsent(JobUserType.EXEC_USER.getValue(), k -> new ArrayList<>())
                                                .add(new NotifyReceiverVo(GroupSearch.USER.getValue(), jobInfo.getExecUser()));
                                    }
                                    flag = true;
                                    NotifyPolicyUtil.execute(
                                            notifyPolicyVo.getHandler(),
                                            notifyTriggerType,
                                            DeployJobMessageHandler.class,
                                            notifyPolicyVo,
                                            null,
                                            null,
                                            receiverMap,
                                            jobInfo,
                                            null,
                                            notifyAuditMessageStringBuilder.toString()
                                    );
                                } else {
                                    notifyAuditMessageStringBuilder.append(" 通知策略config为null，不触发通知");
                                }
                            }
                        }
                    } else {
                        notifyAuditMessageStringBuilder.append(" 在deploy_job表中该作业的app_system_id为null").append("，无法触发通知");
                    }
                } else {
                    notifyAuditMessageStringBuilder
                            .append(" 作业")
                            .append(jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY)
                            .append("(")
                            .append(jobVo.getId())
                            .append(")")
                            .append("的状态为")
                            .append(jobVo.getStatus());
                    notifyAuditMessageStringBuilder.append(" 在deploy_job表中找不到该作业的数据").append("，无法触发通知");
                }
            } else {
                notifyAuditMessageStringBuilder
                        .append("根据作业")
                        .append(jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY)
                        .append("(")
                        .append(jobVo.getId())
                        .append(")")
                        .append("的状态")
                        .append(jobVo.getStatus())
                        .append("，找不到触发点")
                        .append("，无法触发通知");
            }
        } catch (Exception ex) {
            logger.error("发布作业：" + jobVo.getId() + "-" + (jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY) + "通知失败");
            logger.error(ex.getMessage(), ex);
        } finally {
            if (!flag) {
                Logger notifyAuditLogger = LoggerFactory.getLogger("notifyAudit");
                notifyAuditLogger.info("\n" + notifyAuditMessageStringBuilder);
            }
        }
    }
}
