/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.trigger.DeployJobTriggerAppModuleVo;
import codedriver.framework.deploy.dto.trigger.DeployJobTriggerAuditVo;
import codedriver.framework.deploy.dto.trigger.DeployJobTriggerVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployJobTriggerMapper {
    int getTriggerCount(DeployJobTriggerVo deployJobTriggerVo);

    List<DeployJobTriggerVo> searchTrigger(DeployJobTriggerVo deployJobTriggerVo);

    List<DeployJobTriggerVo> getTriggerListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    int checkTriggerNameIsExist(@Param("id") Long id, @Param("name") String name);

    DeployJobTriggerVo getTriggerById(Long id);

    int getTriggerAuditCount(DeployJobTriggerAuditVo deployJobTriggerAuditVo);

    List<DeployJobTriggerAuditVo> searchTriggerAudit(DeployJobTriggerAuditVo deployJobTriggerAuditVo);

    int insertJobTrigger(DeployJobTriggerVo deployJobTriggerVo);

    int insertJobTriggerAppModule(DeployJobTriggerAppModuleVo triggerAppModuleVo);

    int updateJobTrigger(DeployJobTriggerVo deployJobTriggerVo);

    int insertJobTriggerAudit(DeployJobTriggerAuditVo triggerAuditVo);

    int deleteTriggerById(Long id);

    int deleteTriggerAuditByTriggerId(Long id);
}
