package codedriver.module.deploy.dao.mapper;


import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.deploy.crossover.IDeployProfileCrossoverMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileOperationVo;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/16 11:42 上午
 */
public interface DeployProfileMapper extends IDeployProfileCrossoverMapper {

    int searchDeployProfileCount(DeployProfileVo profileVo);

    int checkProfileIsExists(Long id);

    int checkProfileNameIsRepeats(DeployProfileVo vo);

    List<Long> getDeployProfileIdList(DeployProfileVo profileVo);

    List<DeployProfileVo> getProfileListByIdList(@Param("idList") List<Long> idList);

    List<DeployProfileOperationVo> getProfileOperationVoListByProfileId(Long id);

    DeployProfileVo getProfileVoById(Long id);

    void insertDeployProfileOperation(@Param("profileId") Long profileId, @Param("operationIdList") List<Long> operationIdList, @Param("type") String type);

    void insertProfile(DeployProfileVo profileVo);

    void updateProfile(DeployProfileVo profileVo);

    void deleteProfileById(Long id);

    void deleteProfileOperationByProfileId(Long id);

    void deleteProfileOperationByOperationId(Long id);

    List<AutoexecOperationVo> getAutoexecOperationVoByProfileId(Long id);

}
