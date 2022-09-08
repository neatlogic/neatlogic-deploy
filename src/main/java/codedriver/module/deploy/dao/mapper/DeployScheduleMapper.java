/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;

import java.util.List;

public interface DeployScheduleMapper {
    DeployScheduleVo getScheduleById(Long id);

//    DeployScheduleVo getPipelineScheduleById(Long id);

    int checkScheduleNameIsExists(DeployScheduleVo vo);

//    int checkPipelineScheduleNameIsExists(DeployScheduleVo vo);

    int getScheduleCount(DeployScheduleVo searchVo);

    List<DeployScheduleVo> getScheduleList(DeployScheduleVo searchVo);

    List<DeployScheduleVo> getScheduleListByIdList(List<Long> idList);

//    int getPipelineScheduleCount(DeployScheduleVo searchVo);
//
//    List<DeployScheduleVo> getPipelineScheduleList(DeployScheduleVo searchVo);
//
//    List<DeployScheduleVo> getPipelineScheduleByIdList(List<Long> idList);

//    List<Long> getMergeScheduleIdList(DeployScheduleVo searchVo);

    int insertSchedule(DeployScheduleVo scheduleVo);

//    int insertPipelineSchedule(DeployScheduleVo scheduleVo);

    int updateSchedule(DeployScheduleVo scheduleVo);

//    int updatePipelineSchedule(DeployScheduleVo scheduleVo);

    int updateScheduleIsActiveById(Long id);

//    int updatePipelineScheduleIsActiveById(Long id);

    int deleteScheduleById(Long id);

//    int deletePipelineScheduleById(Long id);
}
