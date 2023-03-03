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

import neatlogic.framework.deploy.dto.schedule.DeployScheduleSearchVo;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;

import java.util.List;

public interface DeployScheduleMapper {
    DeployScheduleVo getScheduleById(Long id);

    DeployScheduleVo getScheduleByUuid(String uuid);

    int checkScheduleNameIsExists(DeployScheduleVo vo);

    int getScheduleCount(DeployScheduleSearchVo searchVo);

    List<DeployScheduleVo> getScheduleList(DeployScheduleSearchVo searchVo);

    List<DeployScheduleVo> getScheduleListByIdList(List<Long> idList);

    List<DeployScheduleVo> getScheduleAuditCountListByIdList(List<Long> idList);

    int insertSchedule(DeployScheduleVo scheduleVo);

    int updateSchedule(DeployScheduleVo scheduleVo);

    int updateScheduleIsActiveById(Long id);

    int deleteScheduleById(Long id);
}
