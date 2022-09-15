/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;

import java.util.List;

public interface DeployScheduleMapper {
    DeployScheduleVo getScheduleById(Long id);

    DeployScheduleVo getScheduleByUuid(String uuid);

    int checkScheduleNameIsExists(DeployScheduleVo vo);

    int getScheduleCount(DeployScheduleVo searchVo);

    List<DeployScheduleVo> getScheduleList(DeployScheduleVo searchVo);

    List<DeployScheduleVo> getScheduleListByIdList(List<Long> idList);

    List<DeployScheduleVo> getScheduleAuditCountListByIdList(List<Long> idList);

    int insertSchedule(DeployScheduleVo scheduleVo);

    int updateSchedule(DeployScheduleVo scheduleVo);

    int updateScheduleIsActiveById(Long id);

    int deleteScheduleById(Long id);
}
