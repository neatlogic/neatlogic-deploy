package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.scenario.DeployScenarioVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/15 12:13 下午
 */
public interface DeployScenarioMapper {

    int checkScenarioIsExistsById(Long paramId);

    int getScenarioCount(DeployScenarioVo paramScenarioVo);

    DeployScenarioVo getScenarioById(Long paramId);

    List<Long> getScenarioIdList(DeployScenarioVo paramScenarioVo);

    List<DeployScenarioVo> getScenarioListByIdList(@Param("idList") List<Long> idList);

    void insertScenario(DeployScenarioVo paramScenarioVo);

    int checkScenarioNameIsRepeat(DeployScenarioVo paramScenarioVo);

    void deleteScenarioById(Long paramId);
}
