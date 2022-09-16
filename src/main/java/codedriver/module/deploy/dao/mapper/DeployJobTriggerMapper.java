/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.trigger.DeployJobTriggerVo;

import java.util.List;

public interface DeployJobTriggerMapper {
    int getTriggerCount(DeployJobTriggerVo deployJobTriggerVo);

    List<Long> getTriggerIdList(DeployJobTriggerVo deployJobTriggerVo);

    List<DeployJobTriggerVo> getTriggerListByIdList(List<Long> idList);
}
