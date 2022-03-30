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
     * 保存profile和tool、script的关系
     * 在删除profile时会删除此关系，在删除script的时候也会删除此关系
     *
     * @param profileId
     * @param autoexecOperationVoList
     */
    void saveProfileOperationByProfileIdAndAutoexecOperationVoList(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList);

}
