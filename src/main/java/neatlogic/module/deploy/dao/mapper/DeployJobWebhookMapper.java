/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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
