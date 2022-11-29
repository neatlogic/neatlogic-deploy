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
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    PipelineService pipelineService;

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
                                attrInfo.put("isMultiple", attrVoConfig.getInteger("isMultiple") == 1);
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
