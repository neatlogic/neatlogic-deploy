package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import codedriver.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/27 6:03 下午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

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
            @Param(name = "instanceIdList", type = ApiParamType.JSONARRAY, desc = "实例id"),
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

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

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

        paramObj.put("needUpdateRelList", new JSONArray(Arrays.asList("APPComponent")));

        //实例挂环境
        JSONArray instanceIdArray = paramObj.getJSONArray("instanceIdList");
        if (CollectionUtils.isNotEmpty(instanceIdArray)) {
            List<Long> instanceIdList = instanceIdArray.toJavaList(Long.class);
            for (Long instanceId : instanceIdList) {
                //获取实例的具体信息
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                CiEntityVo instanceCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(instanceId);
                if (instanceCiEntity == null) {
                    throw new CiEntityNotFoundException(instanceId);
                }
                ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
                CiEntityVo instanceCiEntityInfo = ciEntityService.getCiEntityById(instanceCiEntity.getCiId(), instanceId);

                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(instanceCiEntityInfo);
                JSONObject attrEntityData = instanceCiEntityInfo.getAttrEntityData();
                ciEntityTransactionVo.setAttrEntityData(attrEntityData);

                //添加环境属性、模块关系
                paramObj.put("ciId", instanceCiEntity.getCiId());
                paramObj.put("needUpdateAttrList", new JSONArray(Arrays.asList("app_environment")));
                deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

                //设置基础信息
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setEditMode(EditModeType.GLOBAL.getValue());

                List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                ciEntityTransactionList.add(ciEntityTransactionVo);
                ciEntityService.saveCiEntity(ciEntityTransactionList);
            }
        } else {

            //新增实例到cmdb
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo paramCiVo = ciCrossoverMapper.getCiById(paramObj.getLong("ciId"));
            if (paramCiVo == null) {
                throw new CiNotFoundException(paramObj.getLong("ciId"));
            }
            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();

            //添加环境属性、模块关系
            paramObj.put("needUpdateAttrList", new JSONArray(Arrays.asList("name", "ip", "port", "maintenance_window", "app_environment")));
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            ciEntityTransactionList.add(ciEntityTransactionVo);
            ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }

        return null;
    }
}
