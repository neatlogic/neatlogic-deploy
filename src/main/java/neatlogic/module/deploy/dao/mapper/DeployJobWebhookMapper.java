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

package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookAppModuleVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployJobWebhookMapper {
    int getWebhookCount(DeployJobWebhookVo deployJobTriggerVo);

    List<DeployJobWebhookVo> searchWebhook(DeployJobWebhookVo deployJobTriggerVo);

    List<DeployJobWebhookVo> getWebhookListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    int checkWebhookNameIsExist(@Param("id") Long id, @Param("name") String name);

    DeployJobWebhookVo getWebhookById(Long id);

    int getWebhookAuditCount(DeployJobWebhookAuditVo deployJobTriggerAuditVo);

    List<DeployJobWebhookAuditVo> searchWebhookAudit(DeployJobWebhookAuditVo deployJobTriggerAuditVo);

    int insertJobWebhook(DeployJobWebhookVo deployJobTriggerVo);

    int insertJobWebhookAppModule(DeployJobWebhookAppModuleVo triggerAppModuleVo);

    int updateJobWebhook(DeployJobWebhookVo deployJobTriggerVo);

    int insertJobWebhookAudit(DeployJobWebhookAuditVo triggerAuditVo);

    int deleteWebhookById(Long id);

    int deleteWebhookAuditByWebhookId(Long id);

    void ActivateJobWebhookById(@Param("id") Long id, @Param("isActive") Integer isActive);

    void deleteWebhookByIdAppModuleByWebhookId(Long id);
}
