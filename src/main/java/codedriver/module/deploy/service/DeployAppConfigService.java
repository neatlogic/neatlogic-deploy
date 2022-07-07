package codedriver.module.deploy.service;

import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import com.alibaba.fastjson.JSONObject;

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
    void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj);

}
