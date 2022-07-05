package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.*;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/6/27 6:03 下午
 */
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "保存发布应用配置的应用模块环境的实例";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/save";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "实例id"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "维护窗口")
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("envId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("envId"));
        }

        //实例挂环境
        Long instanceId = paramObj.getLong("instanceId");
        if (instanceId != null) {

            //获取实例的具体信息
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(instanceId);
            ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            CiEntityVo instanceCiEntityInfo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), instanceId);

            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(instanceCiEntityInfo);

            //添加环境属性
            IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
            AttrVo appEnvironmentAttrVo = attrCrossoverMapper.getAttrByCiIdAndName(instanceCiEntity.getCiId(), "app_environment");
            JSONObject attrEntityData = instanceCiEntityInfo.getAttrEntityData();
            ciEntityTransactionVo.addAttrEntityData(appEnvironmentAttrVo, paramObj.getString("envId"));

            //添加模块关系
            paramObj.put("ciId", instanceCiEntity.getCiId());
            addRelEntityData(ciEntityTransactionVo, paramObj);

            //设置基础信息
            ciEntityTransactionVo.setCiId(instanceCiEntityInfo.getCiId());
            ciEntityTransactionVo.setCiEntityId(instanceCiEntityInfo.getId());
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setAllowCommit(true);
            ciEntityTransactionVo.setAttrEntityData(attrEntityData);
            ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());

            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            ciEntityTransactionList.add(ciEntityTransactionVo);
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        } else {
            //新增实例到cmdb

            //判断模型是否存在
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo paramCiVo = ciCrossoverMapper.getCiById(paramObj.getLong("ciId"));
            if (paramCiVo == null) {
                throw new CiNotFoundException(paramObj.getLong("ciId"));
            }
            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();

            //添加属性和关系
            addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            ciEntityTransactionList.add(ciEntityTransactionVo);
            ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }

        return null;
    }

    /**
     * 添加属性和关系
     *
     * @param ciEntityTransactionVo 配置项
     * @param paramObj              入参
     */
    private void addAttrEntityDataAndRelEntityData(CiEntityTransactionVo ciEntityTransactionVo, JSONObject paramObj) {

        //添加属性
        addAttrEntityData(ciEntityTransactionVo, paramObj);
        //添加关系
        addRelEntityData(ciEntityTransactionVo, paramObj);
        //设置基础信息
        ciEntityTransactionVo.setCiId(paramObj.getLong("ciId"));
        ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        ciEntityTransactionVo.setAllowCommit(true);
        ciEntityTransactionVo.setDescription(null);
        ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
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

        RelVo aPPComponentRel = relCrossoverMapper.getRelByCiIdAndRelName(paramObj.getLong("ciId"), "APPComponent");
        if (aPPComponentRel == null) {
            return;
        }
        CiEntityVo appModuleCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId"));
        ciEntityTransactionVo.addRelEntityData(aPPComponentRel, aPPComponentRel.getDirection(), appModuleCiEntity.getCiId(), appModuleCiEntity.getId());
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
            if (getAttrMap().containsKey(attrVo.getName())) {
                ciEntityTransactionVo.addAttrEntityData(attrVo, paramObj.getString(getAttrMap().get(attrVo.getName())));
                continue;
            }
            ciEntityTransactionVo.addAttrEntityData(attrVo);
        }
    }

    public static Map<String,String> getAttrMap() {
        Map<String,String> map = new HashMap<>();
        map.put("name", "name");
        map.put("ip", "ip");
        map.put("maintenance_window", "maintenanceWindow");
        map.put("port", "port");
        map.put("app_environment", "envId");
        return map;
    }


}
