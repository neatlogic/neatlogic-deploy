/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.callback;

import codedriver.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployRequestFrom;
import codedriver.framework.deploy.constvalue.DeployWebhookBuildNoPolicy;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookVo;
import codedriver.framework.exception.integration.IntegrationHandlerNotFoundException;
import codedriver.framework.integration.core.IIntegrationHandler;
import codedriver.framework.integration.core.IntegrationHandlerFactory;
import codedriver.framework.integration.dao.mapper.IntegrationMapper;
import codedriver.framework.integration.dto.IntegrationResultVo;
import codedriver.framework.integration.dto.IntegrationVo;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployJobWebhookMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/9/19 17:40
 **/
@Component
public class DeployJobWebhookCallbackHandler extends AutoexecJobCallbackBase {
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private DeployJobWebhookMapper webhookMapper;
    @Resource
    private DeployJobMapper deployJobMapper;
    @Resource
    private IntegrationMapper integrationMapper;

    @Override
    public String getHandler() {
        return DeployJobWebhookCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        if (jobVo != null) {
            AutoexecJobVo autoexecJob = autoexecJobMapper.getJobInfo(jobVo.getId());
            //普通作业触发器
            if (Objects.equals(JobSource.DEPLOY.getValue(), autoexecJob.getSource()) && autoexecJob.getParentId() == null) {
                //作业回调
                AutoexecJobInvokeVo jobInvokeVo = autoexecJobMapper.getJobInvokeByJobId(jobVo.getId());
                if (jobInvokeVo != null) {
                    DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobInvokeVo.getInvokeId());
                    if (deployJobVo != null) {
                        List<DeployJobWebhookVo> webhookVoList = webhookMapper.getWebhookListByAppSystemIdAndAppModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
                        for (DeployJobWebhookVo webhookVo : webhookVoList) {
                            if (webhookVo.getConfig().getEnvNameList().contains(deployJobVo.getEnvName())
                                    && webhookVo.getConfig().getJobStatusList().contains(jobVo.getStatus())) {
                                return true;
                            }
                        }
                    }
                }
            } else if (Objects.equals(autoexecJob.getSource(), JobSource.BATCHDEPLOY.getValue())) {

            }
        }
        return false;
    }


    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(invokeId);
        if (deployJobVo != null) {
            List<DeployJobWebhookVo> webhookVoList = webhookMapper.getWebhookListByAppSystemIdAndAppModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
            for (DeployJobWebhookVo webhookVo : webhookVoList) {
                if (webhookVo.getConfig().getEnvNameList().contains(deployJobVo.getEnvName())
                        && webhookVo.getConfig().getJobStatusList().contains(jobVo.getStatus())) {
                    IntegrationVo integrationVo = integrationMapper.getIntegrationByUuid(webhookVo.getIntegrationUuid());
                    JSONObject param = getIntegrationParam(deployJobVo, webhookVo);
                    integrationVo.getParamObj().putAll(param);
                    IIntegrationHandler handler = IntegrationHandlerFactory.getHandler(integrationVo.getHandler());
                    if (handler == null) {
                        throw new IntegrationHandlerNotFoundException(integrationVo.getHandler());
                    }
                    IntegrationResultVo resultVo = handler.sendRequest(integrationVo, DeployRequestFrom.DEPLOY_TRIGGER);
                    DeployJobWebhookAuditVo webhookAuditVo = new DeployJobWebhookAuditVo(jobVo.getId(),param.getString("jobName"),webhookVo.getId(),resultVo.getAuditId());
                    webhookMapper.insertJobWebhookAudit(webhookAuditVo);
                }
            }
        }
    }

    /**
     * 补充集成接口入参
     *
     * @param deployJobVo 发布作业
     * @param webhookVo   触发器
     * @return 入参
     */
    private JSONObject getIntegrationParam(DeployJobVo deployJobVo, DeployJobWebhookVo webhookVo) {
        JSONObject param = new JSONObject();
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystem = iAppSystemMapper.getAppSystemById(deployJobVo.getAppSystemId());
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getAppSystemId()) == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppSystemId());
        }
        param.put("appSystemId", appSystem.getId());
        param.put("appSystemName", appSystem.getName());
        param.put("appSystemAbbrName", appSystem.getAbbrName());
        AppModuleVo appModule = iAppSystemMapper.getAppModuleById(deployJobVo.getAppModuleId());
        if (appModule == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppModuleId());
        }
        param.put("appModuleId", appModule.getId());
        param.put("appModuleName", appModule.getName());
        param.put("appModuleAbbrName", appModule.getAbbrName());
        CiEntityVo envEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getEnvId());
        if (envEntity == null) {
            throw new CiEntityNotFoundException(deployJobVo.getEnvId());
        }
        param.put("envName", envEntity.getName());
        if (deployJobVo.getBuildNo() != null) {
            if (Objects.equals(DeployWebhookBuildNoPolicy.NEW.getValue(), webhookVo.getBuildNoPolicy())) {
                param.put("buildNo", -1);
            } else if (Objects.equals(DeployWebhookBuildNoPolicy.THE_SAME.getValue(), webhookVo.getBuildNoPolicy())) {
                param.put("buildNo", deployJobVo.getBuildNo());
            }
        }
        String targetScenarioName = webhookVo.getConfig().getIntegrationInputParam().getString("scenarioName");
        if (StringUtils.isBlank(targetScenarioName)) {
            IAutoexecScenarioCrossoverMapper autoexecScenarioCrossoverMapper = CrossoverServiceFactory.getApi(IAutoexecScenarioCrossoverMapper.class);
            AutoexecScenarioVo scenarioVo = autoexecScenarioCrossoverMapper.getScenarioById(deployJobVo.getScenarioId());
            if (scenarioVo == null) {
                throw new AutoexecScenarioIsNotFoundException(deployJobVo.getScenarioId());
            }
            param.put("scenarioName", scenarioVo.getName());
        } else {
            param.put("scenarioName", targetScenarioName);
        }
        param.put("targetEnvName", webhookVo.getConfig().getIntegrationInputParam().getString("targetEnvName"));
        param.put("versionId", deployJobVo.getVersionId());
        param.put("version", deployJobVo.getVersion());
        param.put("jobName", appSystem.getAbbrName() + "/" + appModule.getAbbrName() + "/" + envEntity.getName() + (StringUtils.isBlank(deployJobVo.getVersion()) ? StringUtils.EMPTY : "/" + deployJobVo.getVersion()));
        return param;
    }
}
