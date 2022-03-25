package codedriver.module.deploy.service;

import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;

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
     * @param toolIdList   工具id
     * @param scriptIdList 脚本id
     * @param paramList    工具参数
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList,  List<AutoexecParamVo> paramList);

    /**
     * 根据profileId查询关联的tool、script工具
     *
     * @param id
     * @return
     */
    List<AutoexecToolAndScriptVo> getAutoexecToolAndScriptVoListByProfileId(Long id);
}
