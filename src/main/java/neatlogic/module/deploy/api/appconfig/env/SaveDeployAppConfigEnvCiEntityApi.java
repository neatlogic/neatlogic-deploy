package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployAppConfigEnvCiEntityApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdaae.savedeployappconfigenvdbapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/cientity/save";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "term.cmdb.ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.port"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name"),
//            @Param(name = "attrList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "common.attributelist"),
//            @Param(name = "relList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.rellist"),
//            @Param(name = "globalAttrList", type = ApiParamType.JSONARRAY, desc = "nmdaae.savedeployappconfigenvcientityapi.input.param.desc.globalattrlist"),
    })
    @Output({
            @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid")
    })
    @Description(desc = "nmdaae.savedeployappconfigenvdbapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, envId);
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(envId);
        }
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        //新增实例到cmdb
        Long ciId = paramObj.getLong("ciId");
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
//        List<String> attrList = new ArrayList<>();
//        List<String> relList = new ArrayList<>();
//        List<String> globalAttrList = new ArrayList<>();
//        JSONArray attrArray = paramObj.getJSONArray("attrList");
//        if (CollectionUtils.isNotEmpty(attrArray)) {
//            attrList = attrArray.toJavaList(String.class);
//        }
//        JSONArray relArray = paramObj.getJSONArray("relList");
//        if (CollectionUtils.isNotEmpty(relArray)) {
//            relList = relArray.toJavaList(String.class);
//        }
//        JSONArray globalAttrArray = paramObj.getJSONArray("globalAttrList");
//        if (CollectionUtils.isNotEmpty(globalAttrArray)) {
//            globalAttrList = globalAttrArray.toJavaList(String.class);
//        }
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        Long id = paramObj.getLong("id");
        if (id != null) {
            //获取实例的具体信息
            CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(id);
            if (instanceCiEntity == null) {
                throw new CiEntityNotFoundException(id);
            }
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), id);
            ciEntityTransactionVo.setCiEntityId(ciEntityVo.getId());
            ciEntityTransactionVo.setCiId(ciEntityVo.getCiId());
            ciEntityTransactionVo.setAllowCommit(true);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());
            JSONObject attrEntityData = ciEntityVo.getAttrEntityData();
            if (MapUtils.isNotEmpty(attrEntityData)) {
                ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(attrEntityData.toJSONString()));
            }
            JSONObject relEntityData = ciEntityVo.getRelEntityData();
            if (MapUtils.isNotEmpty(relEntityData)) {
                ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(relEntityData.toJSONString()));
            }
            JSONObject globalAttrEntityData = ciEntityVo.getGlobalAttrEntityData();
            if (MapUtils.isNotEmpty(globalAttrEntityData)) {
                ciEntityTransactionVo.setGlobalAttrEntityData(JSONObject.parseObject(globalAttrEntityData.toJSONString()));
            }
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            //修改属性值
            deployAppConfigService.addAttrEntityDataAndRelEntityData(
                    ciEntityTransactionVo,
                    ciVo.getId(),
                    paramObj,
                    Arrays.asList("name", "ip", "port", "maintenance_window"),
                    Collections.singletonList("APPComponent"),
                    Collections.singletonList("app_environment")
            );
        } else {
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            //添加环境属性、模块关系
            deployAppConfigService.addAttrEntityDataAndRelEntityData(
                    ciEntityTransactionVo,
                    ciVo.getId(),
                    paramObj,
                    Arrays.asList("name", "ip", "port", "maintenance_window"),
                    Collections.singletonList("APPComponent"),
                    Collections.singletonList("app_environment")
            );
        }
        ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
        JSONObject resultObj = new JSONObject();
        Long ciEntityId = ciEntityTransactionVo.getCiEntityId();
        resultObj.put("id", ciEntityId);
        resultObj.put("transactionGroupId", transactionGroupId);
        return resultObj;
    }
}
