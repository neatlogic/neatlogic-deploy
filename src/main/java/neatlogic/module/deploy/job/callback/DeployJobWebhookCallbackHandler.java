/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.job.callback;

import neatlogic.framework.autoexec.crossover.IAutoexecScenarioCrossoverMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployRequestFrom;
import neatlogic.framework.deploy.constvalue.DeployWebhookBuildNoPolicy;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import neatlogic.framework.exception.integration.IntegrationHandlerNotFoundException;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
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
