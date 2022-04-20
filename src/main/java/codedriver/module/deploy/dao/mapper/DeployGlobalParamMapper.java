package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.param.DeployGlobalParamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/18 7:02 下午
 */
public interface DeployGlobalParamMapper {

    int checkGlobalParamIsExistsById(Long paramId);

    DeployGlobalParamVo getGlobalParamById(Long paramId);

    int getGlobalParamCount(DeployGlobalParamVo globalParamVo);

    List<Long> getGlobalParamIdList(DeployGlobalParamVo globalParamVo);

    List<DeployGlobalParamVo> getGlobalParamListByIdList(@Param("idList") List<Long> idList);

    List<DeployGlobalParamVo> getGlobalParam(DeployGlobalParamVo globalParamVo);

    int checkGlobalParamNameIsRepeat(DeployGlobalParamVo globalParamVo);

    int checkGlobalParamDisplayNameIsRepeat(DeployGlobalParamVo globalParamVo);

    void insertGlobalParam(DeployGlobalParamVo paramVo);

    void deleteGlobalParamById(Long paramId);
}
