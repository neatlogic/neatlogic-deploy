package codedriver.module.deploy.service;

import codedriver.framework.cmdb.crossover.IAttrCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IRelCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId()));
        deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(configVo.getAppSystemId(), configVo.getAppModuleId(), configVo.getEnvId());
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


    /**
     * 添加关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    private void addRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IRelCrossoverMapper relCrossoverMapper = CrossoverServiceFactory.getApi(IRelCrossoverMapper.class);

        RelVo APPComponentRel = relCrossoverMapper.getRelByCiIdAndRelName(paramObj.getLong("ciId"), "APPComponent");
        if (APPComponentRel == null) {
            return;
        }
        CiEntityVo appModuleCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId"));
        ciEntityTransactionVo.addRelEntityData(APPComponentRel, APPComponentRel.getDirection(), appModuleCiEntity.getCiId(), appModuleCiEntity.getId());
    }

    /**
     * 添加属性
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    void addAttrEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        List<AttrVo> attrVoList = attrCrossoverMapper.getAttrByCiId(paramObj.getLong("ciId"));

        for (AttrVo attrVo : attrVoList) {
            if (CollectionUtils.isNotEmpty(paramObj.getJSONArray("needUpdateAttrList"))) {
                if (getAttrMap().containsKey(attrVo.getName()) && paramObj.getJSONArray("needUpdateAttrList").contains(attrVo.getName())) {
                    ciEntityTransactionVo.addAttrEntityData(attrVo, paramObj.getString(getAttrMap().get(attrVo.getName())));
                }
            } else {
                if (getAttrMap().containsKey(attrVo.getName())) {
                    ciEntityTransactionVo.addAttrEntityData(attrVo, paramObj.getString(getAttrMap().get(attrVo.getName())));
                }
            }
        }
    }

    public static Map<String, String> getAttrMap() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "name");
        map.put("ip", "ip");
        map.put("maintenance_window", "maintenanceWindow");
        map.put("port", "port");
        map.put("app_environment", "envId");
        return map;
    }
}
