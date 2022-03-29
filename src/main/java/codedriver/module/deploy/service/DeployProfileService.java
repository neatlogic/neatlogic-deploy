package codedriver.module.deploy.service;

import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
public interface DeployProfileService {


    /**
     * 根据profileId获取profile参数
     *
     * @param id
     * @return
     */
    List<AutoexecParamVo> getProfileParamById(Long id);

    /**
     * 获取工具参数并去重
     *
     * @param toolIdList            工具id
     * @param scriptIdList          脚本id
     * @param oldOperationParamList 旧工具参数
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, List<AutoexecParamVo> oldOperationParamList);

    /**
     * 根据profileId查询关联的tool、script工具
     *
     * @param id
     * @return
     */
    List<AutoexecOperationVo> getAutoexecOperationVoListByProfileId(Long id);

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId
     * @param autoexecOperationVoList
     */
    void saveProfileOperationByProfileIdAndAutoexecOperationVoList(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList);

}
