package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.cmdb.crossover.*;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrValueIrregularException;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleEnvVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import neatlogic.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author longrf
 * @date 2022/6/30 2:26 下午
 */
@Service
public class DeployAppConfigServiceImpl implements DeployAppConfigService {
    private final Logger logger = LoggerFactory.getLogger(DeployAppConfigServiceImpl.class);
    private final String MYBATIS_MODE = "mybatis";
    private final String JSQLPARSER_MODE = "jsqlparser";
    private final String COMPARISON_ENABLED = "1";

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    PipelineService pipelineService;

    @Resource
    DeployResourceBuildSqlService deployResourceBuildSqlService;

    @Override
    public void deleteAppConfig(DeployAppConfigVo configVo) {

        //删除系统才需要删除权限
        if (configVo.getAppModuleId() == 0L && configVo.getEnvId() == 0L) {
            deployAppConfigMapper.deleteAppConfigAuthorityByAppSystemId(configVo.getAppSystemId());
        }

        //删除系统、模块时才会删除runner组
        if (!(configVo.getAppSystemId() != null && configVo.getAppSystemId() != 0L && configVo.getEnvId() != 0L)) {
            deployAppConfigMapper.deleteAppModuleRunnerGroup(configVo);
        }

        //删除阶段中操作工具对预置参数集和全局参数的引用关系
        List<DeployAppConfigVo> deployAppConfigList = deployAppConfigMapper.getAppConfigList(configVo);
        if (CollectionUtils.isNotEmpty(deployAppConfigList)) {
            for (DeployAppConfigVo config : deployAppConfigList) {
                pipelineService.deleteDependency(config);
            }
        }

        deployAppConfigMapper.deleteAppConfig(configVo);
        deployAppConfigMapper.deleteAppConfigDraft(configVo);
        deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId());
        deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
    }

    @Override
    public void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, Long ciId, JSONObject attrAndRelObj, List<String> needUpdateAttrList, List<String> needUpdateRelList, List<String> needUpdateGlobalAttrList) {

        //添加属性
        addAttrEntityData(ciEntityTransactionVo, attrAndRelObj, needUpdateAttrList, ciId);
        //添加关系
        addRelEntityData(ciEntityTransactionVo, attrAndRelObj, needUpdateRelList, ciId);
        // 添加全局属性
        addGlobalAttrEntityData(ciEntityTransactionVo, attrAndRelObj, needUpdateGlobalAttrList);
        //设置基础信息
        ciEntityTransactionVo.setCiId(ciId);
        ciEntityTransactionVo.setAllowCommit(true);
        ciEntityTransactionVo.setDescription(null);
    }

    @Override
    public void deleteAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject attrAndRelObj, List<String> needDeleteAttrList, List<String> needDeleteRelList, List<String> needDeleteGlobalAttrList) {
        Long ciId = ciEntityTransactionVo.getCiId();
        // 删除属性
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        List<AttrVo> attrVoList = attrCrossoverMapper.getAttrByCiId(ciId);
        for (AttrVo attrVo : attrVoList) {
            if (needDeleteAttrList.contains(attrVo.getName())) {
                String attrParam = getAttrMap().get(attrVo.getName());
                if (StringUtils.isNotBlank(attrParam)) {
                    Object value = attrAndRelObj.get(attrParam);
                    if (value != null) {
                        JSONObject attrEntityDataByAttrId = ciEntityTransactionVo.getAttrEntityDataByAttrId(attrVo.getId());
                        if (MapUtils.isNotEmpty(attrEntityDataByAttrId)) {
                            JSONArray valueList = attrEntityDataByAttrId.getJSONArray("valueList");
                            if (CollectionUtils.isNotEmpty(valueList)) {
                                valueList.remove(value);
                            }
                        }
                    }
                }
            }
        }
        // 删除关系
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IRelCrossoverMapper relCrossoverMapper = CrossoverServiceFactory.getApi(IRelCrossoverMapper.class);
        List<RelVo> relVoList = relCrossoverMapper.getRelByCiId(ciId);
        for (RelVo relVo : relVoList) {
            if (needDeleteRelList.contains(relVo.getFromCiName())) {
                String relParam = getRelMap().get(relVo.getFromCiName());
                if (StringUtils.isNotBlank(relParam)) {
                    Long value = attrAndRelObj.getLong(relParam);
                    if (value != null) {
                        JSONObject relEntityDataByRelIdAndDirection = ciEntityTransactionVo.getRelEntityDataByRelIdAndDirection(relVo.getId(), relVo.getDirection());
                        if (MapUtils.isNotEmpty(relEntityDataByRelIdAndDirection)) {
                            JSONArray valueList = relEntityDataByRelIdAndDirection.getJSONArray("valueList");
                            if (CollectionUtils.isNotEmpty(valueList)) {
                                for (int i = valueList.size() - 1; i >= 0; i--) {
                                    JSONObject valueObj = valueList.getJSONObject(i);
                                    if (MapUtils.isNotEmpty(valueObj)) {
                                        Long ciEntityId = valueObj.getLong("ciEntityId");
                                        if (Objects.equals(ciEntityId, value)) {
                                            valueList.remove(i);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 删除全局属性
        IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
        GlobalAttrVo searchVo = new GlobalAttrVo();
        searchVo.setIsActive(1);
        List<GlobalAttrVo> globalAttrList = globalAttrCrossoverMapper.searchGlobalAttr(searchVo);
        for (GlobalAttrVo globalAttrVo : globalAttrList) {
            if (needDeleteGlobalAttrList.contains(globalAttrVo.getName())) {
                String attrParam = getAttrMap().get(globalAttrVo.getName());
                if (StringUtils.isNotBlank(attrParam)) {
                    Object value = attrAndRelObj.get(attrParam);
                    JSONObject globalAttrEntityDataByAttrId = ciEntityTransactionVo.getGlobalAttrEntityDataByAttrId(globalAttrVo.getId());
                    if (MapUtils.isNotEmpty(globalAttrEntityDataByAttrId)) {
                        JSONArray valueList = globalAttrEntityDataByAttrId.getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            for (int i = valueList.size() - 1; i >= 0; i--) {
                                JSONObject valueObj = valueList.getJSONObject(i);
                                if (MapUtils.isNotEmpty(valueObj)) {
                                    Long id = valueObj.getLong("id");
                                    if (Objects.equals(id, value)) {
                                        valueList.remove(i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<RunnerMapVo> getAppModuleRunnerGroupByAppSystemIdAndModuleId(Long appSystemId, Long appModuleId) {
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemEntity == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        CiEntityVo appModuleEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId);
        if (appModuleEntity == null) {
            throw new CiEntityNotFoundException(appModuleId);
        }
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId, appModuleId);
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystemEntity.getName() + "(" + appSystemId + ")", appModuleEntity.getName() + "(" + appModuleId + ")");
        }
        List<RunnerMapVo> runnerMapList = runnerGroupVo.getRunnerMapList();
        if (com.alibaba.nacos.common.utils.CollectionUtils.isEmpty(runnerMapList)) {
            throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        return runnerMapList;
    }

    @Override
    public Long saveDeployAppModule(DeployAppModuleVo deployAppModuleVo, int isAdd) {
        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployAppModuleVo.getAppSystemId());
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(deployAppModuleVo.getAppSystemId());
        }

        Long appModuleId = deployAppModuleVo.getId();

        JSONObject paramObj = new JSONObject();
        paramObj.put("stateIdList", deployAppModuleVo.getState());
        paramObj.put("ownerIdList", deployAppModuleVo.getOwner());
        paramObj.put("abbrName", deployAppModuleVo.getAbbrName());
        paramObj.put("name", deployAppModuleVo.getName());
        paramObj.put("maintenanceWindow", deployAppModuleVo.getMaintenanceWindow());
        paramObj.put("description", deployAppModuleVo.getDescription());
        paramObj.put("appSystemId", deployAppModuleVo.getAppSystemId());

        //定义需要插入的字段
        List<String> needUpdateAttrList = Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description");
        //获取应用模块的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo moduleCiVo = ciCrossoverMapper.getCiByName("APPComponent");

        //保存
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityTransactionVo ciEntityTransactionVo = null;
        if (isAdd == 1) {

            /*新增应用模块（配置项）*/
            //1、构建事务vo，并添加属性值
            paramObj.put("needUpdateRelList", new JSONArray(Collections.singletonList("APP")));
            ciEntityTransactionVo = new CiEntityTransactionVo();
            addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, ciCrossoverMapper.getCiByName("APPComponent").getId(), paramObj, Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description"), Collections.singletonList("APP"), new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {

            CiEntityVo moduleCiEntityInfo = ciEntityService.getCiEntityById(moduleCiVo.getId(), appModuleId);
            if (moduleCiEntityInfo == null) {
                throw new CiEntityNotFoundException(appModuleId);
            }

            /*编辑应用模块（配置项）*/
            //1、构建事务vo，并添加属性值
            ciEntityTransactionVo = new CiEntityTransactionVo(moduleCiEntityInfo);
            ciEntityTransactionVo.setAttrEntityData(moduleCiEntityInfo.getAttrEntityData());
            addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, moduleCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>(), new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        }
        //3、保存模块（配置项）
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);
        return ciEntityTransactionVo.getCiEntityId();
    }

    @Override
    public JSONObject getDeployCiAttrList(Long ciId, Integer isAll, JSONArray attrNameArray) {
        JSONObject returnObj = new JSONObject();
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        List<AttrVo> attrList = attrCrossoverMapper.getAttrByCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrList)) {
            if (isAll == 0) {
                if (CollectionUtils.isEmpty(attrNameArray)) {
                    throw new ParamIrregularException("attrNameList");
                }
                List<String> attrNameList = attrNameArray.toJavaList(String.class);
                for (int i = 0; i < attrNameList.size(); i++) {
                    String attr = attrNameList.get(i);
                    if (StringUtils.equals(attr, "maintenanceWindow")) {
                        attrNameList.remove(i);
                        attrNameList.add(humpToUnderline(attr));
                    }
                }
                for (AttrVo attrVo : attrList) {
                    JSONObject attrInfo = new JSONObject();
                    if (attrNameList.contains(attrVo.getName())) {
                        attrInfo.put("label", attrVo.getLabel());
                        attrInfo.put("type", attrVo.getType());
                        attrInfo.put("isRequired", attrVo.getIsRequired());
                        if (StringUtils.equals(attrVo.getType(), "select")) {
                            JSONObject attrVoConfig = attrVo.getConfig();
                            if ( MapUtils.isNotEmpty(attrVoConfig)) {
                                attrInfo.put("isMultiple", Objects.equals(attrVoConfig.getInteger("isMultiple") ,1));
                            }
                        } else if (StringUtils.equals(attrVo.getType(), "datetime")) {
                            JSONObject attrVoConfig = attrVo.getConfig();
                            if (attrVoConfig != null) {
                                attrInfo.put("format", attrVoConfig.getString("format"));
                            }
                        } else if (StringUtils.equals(attrVo.getType(), "datetimerange")) {
                            JSONObject attrVoConfig = attrVo.getConfig();
                            if (attrVoConfig != null) {
                                attrInfo.put("format", attrVoConfig.getString("format"));
                                attrInfo.put("formatType", attrVoConfig.getString("type"));
                            }
                        }
                        returnObj.put(StringUtils.equals(attrVo.getName(), "maintenance_window") ? "maintenanceWindow" : attrVo.getName(), attrInfo);
                    }
                }
            } else {
                for (AttrVo attrVo : attrList) {
                    JSONObject attrInfo = new JSONObject();
                    attrInfo.put("label", attrVo.getLabel());
                    attrInfo.put("type", attrVo.getType());
                    attrInfo.put("isRequired", attrVo.getIsRequired());
                    returnObj.put(StringUtils.equals(attrVo.getName(), "maintenance_window") ? "maintenanceWindow" : attrVo.getName(), attrInfo);
                }
            }
        }
        return returnObj;
    }

    @Override
    public ResourceVo getDatabaseById(Long id) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);

        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        ResourceVo newResourceVo = null;
        ResourceVo oldResourceVo = null;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetDatabaseByIdSql(id);
            if (StringUtils.isNotBlank(sql)) {
                newResourceVo = resourceCrossoverMapper.getResourceBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceVo = deployAppConfigMapper.getDatabaseById(id);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            List<ResourceVo> newResourceList = new ArrayList<>();
            if (newResourceVo != null) {
                newResourceVo.setAllIp(null);
                newResourceVo.setBgList(null);
                newResourceVo.setOwnerList(null);
                newResourceList.add(newResourceVo);
            }
            List<ResourceVo> oldResourceList = new ArrayList<>();
            if (oldResourceVo != null) {
                oldResourceList.add(oldResourceVo);
            }
            checkResourceListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceVo;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceVo;
        }
        return null;
    }

    @Override
    public List<DeployAppEnvironmentVo> getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<DeployAppEnvironmentVo> newEnvList = new ArrayList<>();
        List<DeployAppEnvironmentVo> oldEnvList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppEnvListByAppSystemIdAndModuleIdListSql(appSystemId, appModuleIdList);
            if (StringUtils.isNotBlank(sql)) {
                newEnvList = deployAppConfigMapper.getDeployAppEnvListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldEnvList = deployAppConfigMapper.getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkDeployAppEnvironmentListIsEquals(newEnvList, oldEnvList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newEnvList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldEnvList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList) {
        List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
        List<DeployAppEnvironmentVo> configEnvList = deployAppConfigMapper.getConfigDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
        return mergeDeployAppEnvironmentList(cmdbEnvList, configEnvList);
    }

    @Override
    public List<Long> getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(Long appSystemId, List<Long> appModuleIdList) {
        List<Long> cmdbAppModuleIdList = deployAppConfigMapper.getCmdbHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
        List<Long> configAppModuleIdList = deployAppConfigMapper.getConfigHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
        return mergeLongList(cmdbAppModuleIdList, configAppModuleIdList);
    }

    @Override
    public List<Long> getHasEnvAppSystemIdListByAppSystemIdList(List<Long> idList) {
        List<Long> cmdbAppSystemIdList = deployAppConfigMapper.getCmdbHasEnvAppSystemIdListByAppSystemIdList(idList);
        List<Long> configAppSystemIdList = deployAppConfigMapper.getConfigHasEnvAppSystemIdListByAppSystemIdList(idList);
        return mergeLongList(cmdbAppSystemIdList, configAppSystemIdList);
    }

    @Override
    public List<DeployAppModuleEnvVo> getCmdbDeployAppModuleEnvListByAppSystemId(Long appSystemId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<DeployAppModuleEnvVo> newModuleEnvList = new ArrayList<>();
        List<DeployAppModuleEnvVo> oldModuleEnvList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdSql(appSystemId);
            if (StringUtils.isNotBlank(sql)) {
                newModuleEnvList = deployAppConfigMapper.getDeployAppModuleEnvListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldModuleEnvList = deployAppConfigMapper.getCmdbDeployAppModuleEnvListByAppSystemId(appSystemId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkDeployAppModuleEnvListIsEquals(newModuleEnvList, oldModuleEnvList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newModuleEnvList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldModuleEnvList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemId(Long appSystemId) {
        List<DeployAppModuleEnvVo> cmdbModuleEnvList = deployAppConfigMapper.getCmdbDeployAppModuleEnvListByAppSystemId(appSystemId);
        List<DeployAppModuleEnvVo> configModuleEnvList = deployAppConfigMapper.getConfigDeployAppModuleEnvListByAppSystemId(appSystemId);
        return mergeDeployAppModuleEnvList(cmdbModuleEnvList, configModuleEnvList);
    }

    @Override
    public List<DeployAppModuleEnvVo> getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(Long appSystemId, List<Long> appModuleIdList) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<DeployAppModuleEnvVo> newModuleEnvList = new ArrayList<>();
        List<DeployAppModuleEnvVo> oldModuleEnvList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(appSystemId, appModuleIdList);
            if (StringUtils.isNotBlank(sql)) {
                newModuleEnvList = deployAppConfigMapper.getDeployAppModuleEnvList2BySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldModuleEnvList = deployAppConfigMapper.getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, appModuleIdList);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkDeployAppModuleEnvListIsEquals(newModuleEnvList, oldModuleEnvList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newModuleEnvList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldModuleEnvList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(Long appSystemId, List<Long> appModuleIdList) {
        List<DeployAppModuleEnvVo> cmdbModuleEnvList = deployAppConfigMapper.getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, appModuleIdList);
        List<DeployAppModuleEnvVo> configModuleEnvList = deployAppConfigMapper.getConfigDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, appModuleIdList);
        return mergeDeployAppModuleEnvIdList(cmdbModuleEnvList, configModuleEnvList);
    }

    @Override
    public List<AppEnvironmentVo> getDeployAppModuleEnvListByAppSystemIdAndModuleId(Long systemId, Long moduleId) {
        List<AppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbDeployAppModuleEnvListByAppSystemIdAndModuleId(systemId, moduleId);
        List<AppEnvironmentVo> configEnvList = deployAppConfigMapper.getConfigDeployAppModuleEnvListByAppSystemIdAndModuleId(systemId, moduleId);
        return mergeAppEnvironmentList(cmdbEnvList, configEnvList);
    }

    @Override
    public List<DeployAppEnvironmentVo> getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, List<Long> envIdList) {
        List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envIdList);
        List<DeployAppEnvironmentVo> configEnvList = deployAppConfigMapper.getConfigAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envIdList);
        return mergeDeployAppEnvironmentConfigList(cmdbEnvList, configEnvList);
    }

    @Override
    public List<DeployAppEnvironmentVo> getDeployHasNotConfigAppEnvListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, Long envId) {
        List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbDeployHasNotConfigAppEnvListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envId);
        List<DeployAppEnvironmentVo> configEnvList = deployAppConfigMapper.getConfigDeployHasNotConfigAppEnvListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envId);
        return mergeDeployAppEnvironmentList(cmdbEnvList, configEnvList);
    }

    @Override
    public int getAppConfigEnvDatabaseCount(DeployResourceSearchVo searchVo) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newRowNum = 0;
        int oldRowNum = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseCountSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newRowNum = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldRowNum = deployAppConfigMapper.getAppConfigEnvDatabaseCount(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED) && oldRowNum != newRowNum) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("newRowNum", newRowNum);
            errorObj.put("oldRowNum", oldRowNum);
            logger.error("资产清单新旧SQL获取结果不一致：{}", errorObj);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newRowNum;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldRowNum;
        }
        return 0;
    }

    @Override
    public List<Long> getAppConfigEnvDatabaseResourceIdList(DeployResourceSearchVo searchVo) {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseResourceIdListSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newIdList = resourceCrossoverMapper.getIdListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = deployAppConfigMapper.getAppConfigEnvDatabaseResourceIdList(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED) && !Objects.equals(oldIdList, newIdList)) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("newIdList", newIdList);
            errorObj.put("oldIdList", oldIdList);
            logger.error("资产清单新旧SQL获取idList结果不一致：{}", errorObj);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    private boolean checkResourceListIsEquals(List<ResourceVo> resourceList, List<ResourceVo> oldResourceList) {
        if (oldResourceList.size() != resourceList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("resourceList.size()", resourceList.size());
            errorObj.put("oldResourceList.size()", oldResourceList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        resourceList.sort(Comparator.comparing(ResourceVo::getId));
        oldResourceList.sort(Comparator.comparing(ResourceVo::getId));
        for (int i = 0; i < resourceList.size(); i++) {
            ResourceVo resourceVo = resourceList.get(i);
            ResourceVo oldResourceVo = oldResourceList.get(i);
            String resourceString = JSONObject.toJSONString(resourceVo, SerializerFeature.MapSortField);
            String oldResourceString = JSONObject.toJSONString(oldResourceVo, SerializerFeature.MapSortField);
            if (!Objects.equals(resourceString, oldResourceString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("resourceVo", resourceVo);
                errorObj.put("oldResourceVo", oldResourceVo);
                logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }

    private boolean checkDeployAppEnvironmentListIsEquals(List<DeployAppEnvironmentVo> envList, List<DeployAppEnvironmentVo> oldEnvList) {
        if (oldEnvList.size() != envList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("newEnvList.size()", envList.size());
            errorObj.put("oldEnvList.size()", oldEnvList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        for (int i = 0; i < envList.size(); i++) {
            DeployAppEnvironmentVo envVo = envList.get(i);
            DeployAppEnvironmentVo oldEnvVo = oldEnvList.get(i);
            String envString = JSONObject.toJSONString(envVo, SerializerFeature.MapSortField);
            String oldEnvString = JSONObject.toJSONString(oldEnvVo, SerializerFeature.MapSortField);
            if (!Objects.equals(envString, oldEnvString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("envVo", envVo);
                errorObj.put("oldEnvVo", oldEnvVo);
                logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }

    private boolean checkDeployAppModuleEnvListIsEquals(List<DeployAppModuleEnvVo> moduleEnvList, List<DeployAppModuleEnvVo> oldModuleEnvList) {
        if (oldModuleEnvList.size() != moduleEnvList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("newModuleEnvList.size()", moduleEnvList.size());
            errorObj.put("oldModuleEnvList.size()", oldModuleEnvList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        for (int i = 0; i < moduleEnvList.size(); i++) {
            DeployAppModuleEnvVo moduleEnvVo = moduleEnvList.get(i);
            DeployAppModuleEnvVo oldModuleEnvVo = oldModuleEnvList.get(i);
            String moduleEnvString = JSONObject.toJSONString(moduleEnvVo, SerializerFeature.MapSortField);
            String oldModuleEnvString = JSONObject.toJSONString(oldModuleEnvVo, SerializerFeature.MapSortField);
            if (!Objects.equals(moduleEnvString, oldModuleEnvString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("moduleEnvVo", moduleEnvVo);
                errorObj.put("oldModuleEnvVo", oldModuleEnvVo);
                logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }

    @SafeVarargs
    private final List<DeployAppEnvironmentVo> mergeDeployAppEnvironmentList(List<DeployAppEnvironmentVo>... envLists) {
        Map<Long, DeployAppEnvironmentVo> envMap = new LinkedHashMap<>();
        for (List<DeployAppEnvironmentVo> envList : envLists) {
            if (CollectionUtils.isEmpty(envList)) {
                continue;
            }
            for (DeployAppEnvironmentVo env : envList) {
                if (env == null || env.getId() == null) {
                    continue;
                }
                DeployAppEnvironmentVo targetEnv = envMap.computeIfAbsent(env.getId(), key -> {
                    DeployAppEnvironmentVo newEnv = new DeployAppEnvironmentVo();
                    newEnv.setId(env.getId());
                    newEnv.setName(env.getName());
                    newEnv.setAppModuleList(new ArrayList<>());
                    return newEnv;
                });
                if (CollectionUtils.isEmpty(env.getAppModuleList())) {
                    continue;
                }
                Set<Long> appModuleIdSet = new HashSet<>();
                if (CollectionUtils.isNotEmpty(targetEnv.getAppModuleList())) {
                    for (AppModuleVo appModuleVo : targetEnv.getAppModuleList()) {
                        if (appModuleVo != null && appModuleVo.getId() != null) {
                            appModuleIdSet.add(appModuleVo.getId());
                        }
                    }
                }
                for (AppModuleVo appModuleVo : env.getAppModuleList()) {
                    if (appModuleVo != null && appModuleVo.getId() != null && appModuleIdSet.add(appModuleVo.getId())) {
                        targetEnv.getAppModuleList().add(appModuleVo);
                    }
                }
            }
        }
        return new ArrayList<>(envMap.values());
    }

    @SafeVarargs
    private final List<Long> mergeLongList(List<Long>... idLists) {
        Set<Long> idSet = new LinkedHashSet<>();
        for (List<Long> idList : idLists) {
            if (CollectionUtils.isEmpty(idList)) {
                continue;
            }
            for (Long id : idList) {
                if (id != null) {
                    idSet.add(id);
                }
            }
        }
        return new ArrayList<>(idSet);
    }

    @SafeVarargs
    private final List<DeployAppModuleEnvVo> mergeDeployAppModuleEnvList(List<DeployAppModuleEnvVo>... moduleEnvLists) {
        Map<Long, DeployAppModuleEnvVo> moduleEnvMap = new LinkedHashMap<>();
        for (List<DeployAppModuleEnvVo> moduleEnvList : moduleEnvLists) {
            if (CollectionUtils.isEmpty(moduleEnvList)) {
                continue;
            }
            for (DeployAppModuleEnvVo moduleEnvVo : moduleEnvList) {
                if (moduleEnvVo == null || moduleEnvVo.getId() == null) {
                    continue;
                }
                DeployAppModuleEnvVo targetModuleEnv = moduleEnvMap.computeIfAbsent(moduleEnvVo.getId(), key -> {
                    DeployAppModuleEnvVo newModuleEnv = new DeployAppModuleEnvVo();
                    newModuleEnv.setId(moduleEnvVo.getId());
                    newModuleEnv.setEnvList(new ArrayList<>());
                    return newModuleEnv;
                });
                if (CollectionUtils.isEmpty(moduleEnvVo.getEnvList())) {
                    continue;
                }
                Set<Long> envIdSet = new HashSet<>();
                if (CollectionUtils.isNotEmpty(targetModuleEnv.getEnvList())) {
                    for (AppEnvironmentVo envVo : targetModuleEnv.getEnvList()) {
                        if (envVo != null && envVo.getEnvId() != null) {
                            envIdSet.add(envVo.getEnvId());
                        }
                    }
                }
                for (AppEnvironmentVo envVo : moduleEnvVo.getEnvList()) {
                    if (envVo != null && envVo.getEnvId() != null && envIdSet.add(envVo.getEnvId())) {
                        targetModuleEnv.getEnvList().add(envVo);
                    }
                }
            }
        }
        return new ArrayList<>(moduleEnvMap.values());
    }

    @SafeVarargs
    private final List<DeployAppModuleEnvVo> mergeDeployAppModuleEnvIdList(List<DeployAppModuleEnvVo>... moduleEnvLists) {
        Map<Long, DeployAppModuleEnvVo> moduleEnvMap = new LinkedHashMap<>();
        for (List<DeployAppModuleEnvVo> moduleEnvList : moduleEnvLists) {
            if (CollectionUtils.isEmpty(moduleEnvList)) {
                continue;
            }
            for (DeployAppModuleEnvVo moduleEnvVo : moduleEnvList) {
                if (moduleEnvVo == null || moduleEnvVo.getId() == null) {
                    continue;
                }
                DeployAppModuleEnvVo targetModuleEnv = moduleEnvMap.computeIfAbsent(moduleEnvVo.getId(), key -> {
                    DeployAppModuleEnvVo newModuleEnv = new DeployAppModuleEnvVo();
                    newModuleEnv.setId(moduleEnvVo.getId());
                    newModuleEnv.setEnvIdList(new ArrayList<>());
                    return newModuleEnv;
                });
                if (CollectionUtils.isEmpty(moduleEnvVo.getEnvIdList())) {
                    continue;
                }
                Set<Long> envIdSet = new HashSet<>(targetModuleEnv.getEnvIdList());
                for (Long envId : moduleEnvVo.getEnvIdList()) {
                    if (envId != null && envIdSet.add(envId)) {
                        targetModuleEnv.getEnvIdList().add(envId);
                    }
                }
            }
        }
        return new ArrayList<>(moduleEnvMap.values());
    }

    @SafeVarargs
    private final List<AppEnvironmentVo> mergeAppEnvironmentList(List<AppEnvironmentVo>... envLists) {
        Map<Long, AppEnvironmentVo> envMap = new LinkedHashMap<>();
        for (List<AppEnvironmentVo> envList : envLists) {
            if (CollectionUtils.isEmpty(envList)) {
                continue;
            }
            for (AppEnvironmentVo envVo : envList) {
                if (envVo == null || envVo.getEnvId() == null) {
                    continue;
                }
                envMap.putIfAbsent(envVo.getEnvId(), envVo);
            }
        }
        return new ArrayList<>(envMap.values());
    }

    @SafeVarargs
    private final List<DeployAppEnvironmentVo> mergeDeployAppEnvironmentConfigList(List<DeployAppEnvironmentVo>... envLists) {
        Map<Long, DeployAppEnvironmentVo> envMap = new LinkedHashMap<>();
        for (List<DeployAppEnvironmentVo> envList : envLists) {
            if (CollectionUtils.isEmpty(envList)) {
                continue;
            }
            for (DeployAppEnvironmentVo envVo : envList) {
                if (envVo == null || envVo.getId() == null) {
                    continue;
                }
                DeployAppEnvironmentVo targetEnv = envMap.computeIfAbsent(envVo.getId(), key -> {
                    DeployAppEnvironmentVo newEnv = new DeployAppEnvironmentVo();
                    newEnv.setId(envVo.getId());
                    newEnv.setName(envVo.getName());
                    newEnv.setDbSchemaList(new ArrayList<>());
                    newEnv.setAutoCfgKeyValueList(new ArrayList<>());
                    return newEnv;
                });
                mergeDbSchemaList(targetEnv, envVo.getDbSchemaList());
                mergeAutoCfgKeyValueList(targetEnv, envVo.getAutoCfgKeyValueList());
            }
        }
        return new ArrayList<>(envMap.values());
    }

    private void mergeDbSchemaList(DeployAppEnvironmentVo targetEnv, List<DeployAppConfigEnvDBConfigVo> dbSchemaList) {
        if (CollectionUtils.isEmpty(dbSchemaList)) {
            return;
        }
        Set<String> dbSchemaKeySet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(targetEnv.getDbSchemaList())) {
            for (DeployAppConfigEnvDBConfigVo dbConfigVo : targetEnv.getDbSchemaList()) {
                if (dbConfigVo != null && StringUtils.isNotBlank(dbConfigVo.getDbSchema())) {
                    dbSchemaKeySet.add(dbConfigVo.getDbSchema() + "|" + safeString(dbConfigVo.getConfigStr()));
                }
            }
        }
        for (DeployAppConfigEnvDBConfigVo dbConfigVo : dbSchemaList) {
            if (dbConfigVo == null || StringUtils.isBlank(dbConfigVo.getDbSchema())) {
                continue;
            }
            String dbSchemaKey = dbConfigVo.getDbSchema() + "|" + safeString(dbConfigVo.getConfigStr());
            if (dbSchemaKeySet.add(dbSchemaKey)) {
                targetEnv.getDbSchemaList().add(dbConfigVo);
            }
        }
    }

    private void mergeAutoCfgKeyValueList(DeployAppEnvironmentVo targetEnv, List<DeployAppEnvAutoConfigKeyValueVo> autoCfgKeyValueList) {
        if (CollectionUtils.isEmpty(autoCfgKeyValueList)) {
            return;
        }
        Set<String> autoCfgKeySet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(targetEnv.getAutoCfgKeyValueList())) {
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : targetEnv.getAutoCfgKeyValueList()) {
                if (keyValueVo != null && StringUtils.isNotBlank(keyValueVo.getKey())) {
                    autoCfgKeySet.add(buildAutoCfgKey(keyValueVo));
                }
            }
        }
        for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : autoCfgKeyValueList) {
            if (keyValueVo == null || StringUtils.isBlank(keyValueVo.getKey())) {
                continue;
            }
            String autoCfgKey = buildAutoCfgKey(keyValueVo);
            if (autoCfgKeySet.add(autoCfgKey)) {
                targetEnv.getAutoCfgKeyValueList().add(keyValueVo);
            }
        }
    }

    private String buildAutoCfgKey(DeployAppEnvAutoConfigKeyValueVo keyValueVo) {
        return safeString(keyValueVo.getKey()) + "|"
                + safeString(keyValueVo.getType()) + "|"
                + safeString(keyValueVo.getValue()) + "|"
                + Objects.toString(keyValueVo.getIsEmpty(), StringUtils.EMPTY);
    }

    private String safeString(String value) {
        return value == null ? StringUtils.EMPTY : value;
    }

    /**
     * 添加属性
     *
     * @param ciEntityTransactionVo 配置项
     * @param attrAndRelObj         属性信息Obj
     * @param needUpdateRelList     需要更新的关系列表
     * @param ciId                  模型id
     */
    private void addRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject attrAndRelObj, List<String> needUpdateRelList, Long ciId) {

        if (CollectionUtils.isEmpty(needUpdateRelList)) {
            return;
        }

        IRelCrossoverMapper relCrossoverMapper = CrossoverServiceFactory.getApi(IRelCrossoverMapper.class);
        List<RelVo> relVoList = relCrossoverMapper.getRelByCiId(ciId);
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        for (RelVo relVo : relVoList) {
            if (!needUpdateRelList.contains(relVo.getFromCiName())) {
                continue;
            }
            if (StringUtils.isBlank(getRelMap().get(relVo.getFromCiName()))) {
                continue;
            }
            CiEntityVo relCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(attrAndRelObj.getLong(getRelMap().get(relVo.getFromCiName())));
            if (relCiEntity == null) {
                continue;
            }
            ciEntityTransactionVo.addRelEntityData(relVo, relVo.getDirection(), relCiEntity.getCiId(), relCiEntity.getId());
        }
    }

    /**
     * 添加属性
     *
     * @param ciEntityTransactionVo 配置项
     * @param attrAndRelObj         属性信息Obj
     * @param needUpdateAttrList    需要更新的属性列表
     * @param ciId                  模型id
     */
    void addAttrEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject attrAndRelObj, List<String> needUpdateAttrList, Long ciId) {

        if (CollectionUtils.isEmpty(needUpdateAttrList)) {
            return;
        }

        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        List<AttrVo> attrVoList = attrCrossoverMapper.getAttrByCiId(ciId);

        for (AttrVo attrVo : attrVoList) {
            if (!needUpdateAttrList.contains(attrVo.getName())) {
                continue;
            }
            String attrParam = getAttrMap().get(attrVo.getName());
            if (StringUtils.isBlank(attrParam)) {
                continue;
            }
            if (StringUtils.equals(attrVo.getName(), "state") || StringUtils.equals(attrVo.getName(), "owner")) {
                ciEntityTransactionVo.addAttrEntityData(attrVo, CollectionUtils.isNotEmpty(attrAndRelObj.getJSONArray(attrParam)) ? attrAndRelObj.getJSONArray(attrParam) : new JSONArray());
            } else if (StringUtils.equals(attrVo.getName(), "maintenance_window")) {
                JSONArray jsonArray = attrAndRelObj.getJSONArray(attrParam);
                String maintenanceWindowStr = StringUtils.EMPTY;
                if (CollectionUtils.isNotEmpty(jsonArray)) {
                    maintenanceWindowStr = jsonArray.getString(0);
                }
                ciEntityTransactionVo.addAttrEntityData(attrVo, maintenanceWindowStr);
            } else {
                ciEntityTransactionVo.addAttrEntityData(attrVo, attrAndRelObj.getString(attrParam) != null ? attrAndRelObj.getString(attrParam) : "");
            }
        }
    }

    /**
     * 添加全局属性
     *
     * @param ciEntityTransactionVo 配置项
     * @param attrAndRelObj         属性信息Obj
     * @param needUpdateGlobalAttrList    需要更新的全局属性列表
     */
    void addGlobalAttrEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject attrAndRelObj, List<String> needUpdateGlobalAttrList) {
        JSONObject globalAttrEntityData = ciEntityTransactionVo.getGlobalAttrEntityData();
        if (globalAttrEntityData == null) {
            globalAttrEntityData = new JSONObject();
            ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);
        }
        IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
        GlobalAttrVo searchVo = new GlobalAttrVo();
        searchVo.setIsActive(1);
        List<GlobalAttrVo> globalAttrList = globalAttrCrossoverMapper.searchGlobalAttr(searchVo);
        for (GlobalAttrVo globalAttrVo : globalAttrList) {
            if (!needUpdateGlobalAttrList.contains(globalAttrVo.getName())) {
                continue;
            }
            String attrParam = getAttrMap().get(globalAttrVo.getName());
            if (StringUtils.isBlank(attrParam)) {
                continue;
            }
            Long envId = attrAndRelObj.getLong(attrParam);
            if (envId == null) {
                continue;
            }
            GlobalAttrItemVo globalAttrItemVo = globalAttrCrossoverMapper.getGlobalAttrItemById(envId);
            if (globalAttrItemVo == null) {
                throw new GlobalAttrValueIrregularException(globalAttrVo, envId.toString());
            }
            JSONObject globalAttrEntityDataByAttrId = ciEntityTransactionVo.getGlobalAttrEntityDataByAttrId(globalAttrVo.getId());
            if (MapUtils.isNotEmpty(globalAttrEntityDataByAttrId)) {
                boolean flag = false;
                JSONArray valueList = globalAttrEntityDataByAttrId.getJSONArray("valueList");
                for (int i = 0; i < valueList.size(); i++) {
                    JSONObject valueObj = valueList.getJSONObject(i);
                    if (MapUtils.isNotEmpty(valueObj)) {
                        Long id = valueObj.getLong("id");
                        if (Objects.equals(id, globalAttrItemVo.getId())) {
                            flag = true;
                            break;
                        }
                    }
                }
                if (!flag) {
                    JSONObject valueObj = new JSONObject();
                    valueObj.put("id", globalAttrItemVo.getId());
                    valueObj.put("value", globalAttrItemVo.getValue());
                    valueObj.put("sort", globalAttrItemVo.getSort());
                    valueObj.put("attrId", globalAttrVo.getId());
                    valueList.add(valueObj);
                }
            } else {
                JSONArray valueList = new JSONArray();
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("id", globalAttrItemVo.getId());
                jsonObj.put("value", globalAttrItemVo.getValue());
                jsonObj.put("sort", globalAttrItemVo.getSort());
                jsonObj.put("attrId", globalAttrVo.getId());
                valueList.add(jsonObj);
                JSONObject globalAttrEntity = new JSONObject();
                globalAttrEntity.put("valueList", valueList);
                globalAttrEntityData.put("global_" + globalAttrVo.getId(), globalAttrEntity);
            }
        }
    }

    public static Map<String, String> getAttrMap() {
        Map<String, String> map = new HashMap<>();
        //实例
        map.put("name", "name");
        map.put("ip", "ip");
        map.put("maintenance_window", "maintenanceWindow");
        map.put("port", "port");
        map.put("app_environment", "envId");

        //系统
        map.put("state", "stateIdList");
        map.put("owner", "ownerIdList");
        map.put("abbrName", "abbrName");
        map.put("description", "description");
        return map;
    }

    public static Map<String, String> getRelMap() {
        Map<String, String> map = new HashMap<>();
        map.put("APP", "appSystemId");
        map.put("APPComponent", "appModuleId");
        return map;
    }

    /**
     * 驼峰转下划线
     *
     * @param paramString 目标字符串
     * @return java.lang.String
     */
    public static String humpToUnderline(String paramString) {
        String regex = "([A-Z])";
        Matcher matcher = Pattern.compile(regex).matcher(paramString);
        while (matcher.find()) {
            String target = matcher.group();
            paramString = paramString.replaceAll(target, "_" + target.toLowerCase());
        }
        return paramString;
    }
}
