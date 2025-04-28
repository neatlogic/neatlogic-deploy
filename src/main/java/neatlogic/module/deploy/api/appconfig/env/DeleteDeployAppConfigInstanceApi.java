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

/**
 * @author longrf
 * @date 2022/6/27 6:03 下午
 */
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
        return "删除发布应用配置的应用模块环境的实例";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/delete";
    }
//    {"instanceIdList":[1061599224643585],"appSystemId":669482179420161,"appModuleId":669491121676288,"envId":481856650534925,"appSystemName":"TomcatTest","envName":"SIT","moduleName":"WebTest"}
    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "instanceIdList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "实例id列表")
    })
    @Output({
    })
    @Description(desc = "删除发布应用配置的应用模块环境的实例")
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
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            List<Long> instanceIdList = instanceIdArray.toJavaList(Long.class);
            for (Long instanceId : instanceIdList) {
                //获取实例的具体信息
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
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

                //删除环境属性、模块关系
                deployAppConfigService.deleteAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj, Collections.singletonList("app_environment"), Collections.singletonList("APPComponent"), Collections.singletonList("app_environment"));
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }
        return null;
    }
}
