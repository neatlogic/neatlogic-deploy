/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.webhook.DeployJobWebhookAppModuleVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookAuditVo;
import codedriver.framework.deploy.dto.webhook.DeployJobWebhookVo;
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
}
