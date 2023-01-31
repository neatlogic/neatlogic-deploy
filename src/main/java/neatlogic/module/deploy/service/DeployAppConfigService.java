package neatlogic.module.deploy.service;

import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import com.alibaba.fastjson.JSONArray;
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
     * 添加环境等属性、模块等关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param ciId                  模型id
     * @param attrAndRelObj         属性关系Obj
     * @param needUpdateAttrList    需要更新的属性列表
     * @param needUpdateRelList     需要更新的关系列表
     */
    void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, Long ciId, JSONObject attrAndRelObj, List<String> needUpdateAttrList, List<String> needUpdateRelList);

    /**
     * 根据应用模块ID获取runner组
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @return
     */
    List<RunnerMapVo> getAppModuleRunnerGroupByAppSystemIdAndModuleId(Long appSystemId, Long appModuleId);

    /**
     * 保存发布模块配置项信息
     *
     * @param deployAppModuleVo 模块信息
     * @param isAdd             是否新增 1：新增
     * @return 模块id
     */
    Long saveDeployAppModule(DeployAppModuleVo deployAppModuleVo, int isAdd);


    /**
     * 获取发布模型属性列表
     *
     * @param ciId          模型id
     * @param isAll         是否获取所有属性列表
     * @param attrNameArray 需要的属性列表名称
     * @return 属性列表
     */
    JSONObject getDeployCiAttrList(Long ciId, Integer isAll, JSONArray attrNameArray);
}
