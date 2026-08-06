package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeleteDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdaae.deletedeployappconfiginstanceapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/delete";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "instanceIdList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "nmdaae.deletedeployappconfiginstanceapi.input.param.desc.instanceidlist")
    })
    @Output({
    })
    @Description(desc = "nmdaae.deletedeployappconfiginstanceapi.getname")
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
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new CiEntityNotFoundException(appModuleId);
        }
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(envId);
        if (env == null) {
            throw new AppEnvNotFoundException(envId);
        }

        //需要解绑关系的实例
        JSONArray instanceIdArray = paramObj.getJSONArray("instanceIdList");
        if (CollectionUtils.isNotEmpty(instanceIdArray)) {
            ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            List<Long> instanceIdList = instanceIdArray.toJavaList(Long.class);
            for (Long instanceId : instanceIdList) {
                //获取实例的具体信息
                CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(instanceId);
                if (instanceCiEntity == null) {
                    throw new CiEntityNotFoundException(instanceId);
                }
                CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), instanceId);

                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
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

                //删除模块关系
                deployAppConfigService.deleteAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj, new ArrayList<>(), Collections.singletonList("APPComponent"), new ArrayList<>());
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }
        return null;
    }
}
