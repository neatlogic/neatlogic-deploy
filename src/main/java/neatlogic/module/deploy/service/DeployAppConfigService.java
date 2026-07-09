package neatlogic.module.deploy.service;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleEnvVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
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
     * @param needUpdateGlobalAttrList     需要更新的全局属性列表
     */
    void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, Long ciId, JSONObject attrAndRelObj, List<String> needUpdateAttrList, List<String> needUpdateRelList, List<String> needUpdateGlobalAttrList);

    /**
     * 删除环境等属性、模块等关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param attrAndRelObj         属性关系Obj
     * @param needDeleteAttrList    需要删除的属性列表
     * @param needDeleteRelList     需要删除的关系列表
     * @param needDeleteGlobalAttrList     需要删除的全局属性列表
     */
    void deleteAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject attrAndRelObj, List<String> needDeleteAttrList, List<String> needDeleteRelList, List<String> needDeleteGlobalAttrList);

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

    ResourceVo getDatabaseById(Long id);

    List<DeployAppEnvironmentVo> getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList);

    List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList);

    List<Long> getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList);

    List<Long> getHasEnvAppSystemIdListByAppSystemIdList(List<Long> idList);

    List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemId(Long appSystemId);

    List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(Long appSystemId, List<Long> appModuleIdList);

    List<AppEnvironmentVo> getDeployAppModuleEnvListByAppSystemIdAndModuleId(Long systemId, Long moduleId);

    List<DeployAppEnvironmentVo> getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, List<Long> envIdList);

    List<DeployAppEnvironmentVo> getDeployHasNotConfigAppEnvListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, Long envId);

    /**
     * 查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库的数量
     *
     * @param searchVo   searchVo
     * @return count
     */
    int getAppConfigEnvDatabaseCount(DeployResourceSearchVo searchVo);

    List<Long> getAppConfigEnvDatabaseResourceIdList(DeployResourceSearchVo searchVo);
}
