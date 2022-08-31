package codedriver.module.deploy.service;

import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author longrf
 * @date 2022/6/30 2:26 下午
 */
public interface DeployAppConfigService {

    /**
     * 删除发布配置
     *
     * @param configVo configVo
     */
    void deleteAppConfig(DeployAppConfigVo configVo);

    /**
     * 添加环境属性、模块关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, Long ciId, JSONObject paramObj, List<String> needUpdateAttrList, List<String> needUpdateRelList);

    /**
     * 根据应用模块ID获取runner组
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    List<RunnerMapVo> getAppModuleRunnerGroupByAppSystemIdAndModuleId(Long appSystemId, Long appModuleId);

    Long saveDeployAppModule(DeployAppModuleVo deployAppModuleVo, int isAdd);
}
