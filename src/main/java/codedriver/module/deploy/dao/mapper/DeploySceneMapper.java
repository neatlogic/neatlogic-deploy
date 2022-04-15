package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.scene.DeploySceneVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/15 12:13 下午
 */
public interface DeploySceneMapper {

    int checkSceneIsExistsById(Long paramId);

    int getSceneCount(DeploySceneVo paramSceneVo);

    DeploySceneVo getSceneById(Long paramId);

    List<Long> getSceneIdList(DeploySceneVo paramSceneVo);

    List<DeploySceneVo> getSceneListByIdList(@Param("idList") List<Long> idList);

    void insertScene(DeploySceneVo paramSceneVo);

}
