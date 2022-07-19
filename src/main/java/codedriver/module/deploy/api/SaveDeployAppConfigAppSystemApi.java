package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppSystemOwnerVo;
import codedriver.framework.deploy.dto.app.DeployAppSystemStateVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
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
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Override
    public String getName() {
        return "保存发布应用配置的应用系统";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/save";
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
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注")
    })
    @Output({})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long appSystemId = paramObj.getLong("id");

        //构建数据结构
        JSONArray stateArray = paramObj.getJSONArray("stateList");
        if (CollectionUtils.isNotEmpty(stateArray)) {
            List<DeployAppSystemStateVo> stateList = stateArray.toJavaList(DeployAppSystemStateVo.class);
            paramObj.put("stateIdList", stateList.stream().map(DeployAppSystemStateVo::getId).collect(Collectors.toList()));
        } else {
            paramObj.put("stateIdList", new ArrayList<>());
        }
        JSONArray ownerArray = paramObj.getJSONArray("ownerList");
        if (CollectionUtils.isNotEmpty(ownerArray)) {
            List<DeployAppSystemOwnerVo> stateList = ownerArray.toJavaList(DeployAppSystemOwnerVo.class);
            paramObj.put("ownerIdList", stateList.stream().map(DeployAppSystemOwnerVo::getId).collect(Collectors.toList()));
        } else {
            paramObj.put("ownerIdList", new ArrayList<>());
        }

        //定义需要插入的字段
        paramObj.put("needUpdateAttrList", new JSONArray(Arrays.asList("state", "name", "owner", "abbrName", "maintenanceWindow", "description")));
        //获取应用系统的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APP");
        paramObj.put("ciId", appCiVo.getId());

        //保存
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityTransactionVo ciEntityTransactionVo = null;
        if (appSystemId == null) {

            /*新增应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            ciEntityTransactionVo = new CiEntityTransactionVo();
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, paramObj);

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {

            /*编辑应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            CiEntityVo instanceCiEntityInfo = ciEntityService.getCiEntityById(appCiVo.getId(), appSystemId);
            ciEntityTransactionVo = new CiEntityTransactionVo(instanceCiEntityInfo);
            ciEntityTransactionVo.setAttrEntityData(instanceCiEntityInfo.getAttrEntityData());
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
