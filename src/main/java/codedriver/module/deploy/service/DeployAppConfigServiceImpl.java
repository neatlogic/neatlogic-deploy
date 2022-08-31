package codedriver.module.deploy.service;

import codedriver.framework.cmdb.crossover.*;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/6/30 2:26 下午
 */
@Service
public class DeployAppConfigServiceImpl implements DeployAppConfigService {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public void deleteAppConfig(DeployAppConfigVo configVo) {

        //删除系统才需要删除权限
        if (configVo.getAppModuleId() == 0L && configVo.getEnvId() == 0L) {
            deployAppConfigMapper.deleteAppConfigAuthorityByAppSystemId(configVo.getAppSystemId());
        }

        //删除系统、模块时才会删除runner组
        if (!(configVo.getAppSystemId() != 0L && configVo.getAppSystemId() != 0L && configVo.getEnvId() != 0L)) {
            deployAppConfigMapper.deleteAppModuleRunnerGroup(configVo);
        }

        deployAppConfigMapper.deleteAppConfig(configVo);
        deployAppConfigMapper.deleteAppConfigDraft(configVo);
        deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId());
        deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
    }

    @Override
    public void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, Long ciId, JSONObject attrAndRelObj, List<String> needUpdateAttrList, List<String> needUpdateRelList) {

        //添加属性
        addAttrEntityData(ciEntityTransactionVo, attrAndRelObj, needUpdateAttrList, ciId);
        //添加关系
        addRelEntityData(ciEntityTransactionVo, attrAndRelObj, needUpdateRelList, ciId);
        //设置基础信息
        ciEntityTransactionVo.setCiId(ciId);
        ciEntityTransactionVo.setAllowCommit(true);
        ciEntityTransactionVo.setDescription(null);
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
        paramObj.put("stateIdList", deployAppModuleVo.getStateIdList());
        paramObj.put("ownerIdList", deployAppModuleVo.getOwnerIdList());
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
            addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, ciCrossoverMapper.getCiByName("APPComponent").getId(), paramObj, Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description"), Collections.singletonList("APP"));

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
            addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, moduleCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>());

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

    /**
     * 添加关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    private void addRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj, List<String> needUpdateRelList, Long ciId) {

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
            CiEntityVo relCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong(getRelMap().get(relVo.getFromCiName())));
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
     * @param paramObj              入参
     */
    void addAttrEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj, List<String> needUpdateAttrList, Long ciId) {

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
                ciEntityTransactionVo.addAttrEntityData(attrVo, CollectionUtils.isNotEmpty(paramObj.getJSONArray(attrParam)) ? paramObj.getJSONArray(attrParam) : new JSONArray());
            } else if (StringUtils.equals(attrVo.getName(), "maintenance_window")) {
                JSONArray jsonArray = paramObj.getJSONArray(attrParam);
                String maintenanceWindowStr = StringUtils.EMPTY;
                if (CollectionUtils.isNotEmpty(jsonArray)) {
                    maintenanceWindowStr = jsonArray.getString(0);
                }
                ciEntityTransactionVo.addAttrEntityData(attrVo, maintenanceWindowStr);
            } else {
                ciEntityTransactionVo.addAttrEntityData(attrVo, paramObj.getString(attrParam) != null ? paramObj.getString(attrParam) : "");
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
}
