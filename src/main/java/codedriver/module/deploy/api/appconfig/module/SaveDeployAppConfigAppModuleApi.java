package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppOwnerVo;
import codedriver.framework.deploy.dto.app.DeployAppUsedStateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/7/14 4:17 下午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Override
    public String getName() {
        return "保存发布应用配置的应用模块";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appmodule/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "abbrName", type = ApiParamType.STRING, isRequired = true, desc = "简称"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "stateList", type = ApiParamType.JSONARRAY, desc = "状态"),
            @Param(name = "ownerList", type = ApiParamType.JSONARRAY, desc = "负责人"),
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "维护窗口"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({})
    @Description(desc = "保存发布应用配置的应用模块")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }

        Long appModuleId = paramObj.getLong("id");

        //构建数据结构
        JSONArray stateArray = paramObj.getJSONArray("stateList");
        if (CollectionUtils.isNotEmpty(stateArray)) {
            List<DeployAppUsedStateVo> stateList = stateArray.toJavaList(DeployAppUsedStateVo.class);
            paramObj.put("stateIdList", stateList.stream().map(DeployAppUsedStateVo::getId).collect(Collectors.toList()));
        } else {
            paramObj.put("stateIdList", new ArrayList<>());
        }
        JSONArray ownerArray = paramObj.getJSONArray("ownerList");
        if (CollectionUtils.isNotEmpty(ownerArray)) {
            List<DeployAppOwnerVo> stateList = ownerArray.toJavaList(DeployAppOwnerVo.class);
            paramObj.put("ownerIdList", stateList.stream().map(DeployAppOwnerVo::getId).collect(Collectors.toList()));
        } else {
            paramObj.put("ownerIdList", new ArrayList<>());
        }

        //定义需要插入的字段
        paramObj.put("needUpdateAttrList", new JSONArray(Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description")));
        //获取应用系统的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo moduleCiVo = ciCrossoverMapper.getCiByName("APPComponent");
        paramObj.put("ciId", moduleCiVo.getId());

        //保存
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityTransactionVo ciEntityTransactionVo = null;
        if (appModuleId == null) {

            /*新增应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            paramObj.put("needUpdateRelList", new JSONArray(Arrays.asList("APP")));
            ciEntityTransactionVo = new CiEntityTransactionVo();
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {

            CiEntityVo moduleCiEntityInfo = ciEntityService.getCiEntityById(moduleCiVo.getId(), appModuleId);
            if (moduleCiEntityInfo == null) {
                throw new CiEntityNotFoundException(appModuleId);
            }

            /*编辑应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            ciEntityTransactionVo = new CiEntityTransactionVo(moduleCiEntityInfo);
            ciEntityTransactionVo.setAttrEntityData(moduleCiEntityInfo.getAttrEntityData());
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            //2、设置事务vo信息
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        }
        //3、保存系统（配置项）
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);
        return null;
    }
}
