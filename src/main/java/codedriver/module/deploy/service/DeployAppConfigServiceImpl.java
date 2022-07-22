package codedriver.module.deploy.service;

import codedriver.framework.autoexec.exception.AutoexecJobRunnerGroupRunnerNotFoundException;
import codedriver.framework.cmdb.crossover.IAttrCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.crossover.IRelCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
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

        //删除系统、环境需要删除发布存的环境
        if (deployAppConfigMapper.getAppConfigEnv(configVo) > 0) {
            deployAppConfigMapper.deleteAppConfigEnv(configVo);
        }

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
        deployAppConfigMapper.deleteAppEnvAutoConfig(new DeployAppEnvAutoConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
        deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
        deployAppConfigMapper.deleteAppConfigDBConfigAccount(new DeployAppConfigEnvDBConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
    }

    @Override
    public void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {

        //添加属性
        addAttrEntityData(ciEntityTransactionVo, paramObj);
        //添加关系
        addRelEntityData(ciEntityTransactionVo, paramObj);
        //设置基础信息
        ciEntityTransactionVo.setCiId(paramObj.getLong("ciId"));
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
            throw new AutoexecJobRunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        return runnerMapList;
    }

    /**
     * 添加关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    private void addRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {

        if (CollectionUtils.isEmpty(paramObj.getJSONArray("needUpdateRelList"))) {
            return;
        }

        IRelCrossoverMapper relCrossoverMapper = CrossoverServiceFactory.getApi(IRelCrossoverMapper.class);
        List<RelVo> relVoList = relCrossoverMapper.getRelByCiId(paramObj.getLong("ciId"));
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        for (RelVo relVo : relVoList) {
            if (!paramObj.getJSONArray("needUpdateRelList").contains(relVo.getFromCiName())) {
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
    void addAttrEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {

        if (CollectionUtils.isEmpty(paramObj.getJSONArray("needUpdateAttrList"))) {
            return;
        }

        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        List<AttrVo> attrVoList = attrCrossoverMapper.getAttrByCiId(paramObj.getLong("ciId"));

        for (AttrVo attrVo : attrVoList) {
            if (!paramObj.getJSONArray("needUpdateAttrList").contains(attrVo.getName())) {
                continue;
            }
            String attrParam = getAttrMap().get(attrVo.getName());
            if (StringUtils.isBlank(attrParam)) {
                continue;
            }
            if (StringUtils.equals(attrVo.getName(), "state") || StringUtils.equals(attrVo.getName(), "owner")) {
                ciEntityTransactionVo.addAttrEntityData(attrVo, CollectionUtils.isNotEmpty(paramObj.getJSONArray(attrParam)) ? paramObj.getJSONArray(attrParam) : new JSONArray());
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
