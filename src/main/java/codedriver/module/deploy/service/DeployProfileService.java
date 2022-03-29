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
     * 根据关联的operationVoList获取工具参数并与数据库存储的旧参数oldOperationParamList做去重处理
     *
     * @param paramAutoexecOperationVoList
     * @param oldOperationParamList
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList, List<AutoexecParamVo> oldOperationParamList);

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId
     * @param autoexecOperationVoList
     */
    void saveProfileOperationByProfileIdAndAutoexecOperationVoList(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList);

}
