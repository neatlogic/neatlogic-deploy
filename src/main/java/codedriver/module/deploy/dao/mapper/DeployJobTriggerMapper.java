/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.trigger.DeployJobTriggerVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployJobTriggerMapper {
    int getTriggerCount(DeployJobTriggerVo deployJobTriggerVo);

    List<DeployJobTriggerVo> searchTrigger(DeployJobTriggerVo deployJobTriggerVo);

    int checkTriggerNameIsExist(@Param("id") Long id, @Param("name") String name);

    DeployJobTriggerVo getTriggerById(Long id);

    int insertJobTrigger(DeployJobTriggerVo deployJobTriggerVo);

    void updateJobTrigger(DeployJobTriggerVo deployJobTriggerVo);
}
