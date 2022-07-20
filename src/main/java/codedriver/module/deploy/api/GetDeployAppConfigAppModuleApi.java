package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.dto.app.DeployAppOwnerVo;
import codedriver.framework.deploy.dto.app.DeployAppUsedStateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/22 11:44 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "查询应用模块（配置项）信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appmodule/get";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = CiEntityVo[].class, desc = "应用模块（配置项）信息")
    })
    @Description(desc = "查询应用模块（配置项）信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取应用模块ciEntityVo
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("id"));
        DeployAppModuleVo appModuleVo = new DeployAppModuleVo();
        if (ciEntityVo == null) {
            return appModuleVo;
        }
        //获取属性
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("id"));
        appModuleVo.setId(appSystemInfo.getId());
        List<AttrEntityVo> attrEntityList = appSystemInfo.getAttrEntityList();
        if (CollectionUtils.isNotEmpty(attrEntityList)) {
            for (AttrEntityVo attrEntityVo : attrEntityList) {

                //名称
                if (StringUtils.equals(attrEntityVo.getAttrName(), "name")) {
                    appModuleVo.setName(String.valueOf(attrEntityVo.getValueList().get(0)));
                    continue;
                }
                //简称
                if (StringUtils.equals(attrEntityVo.getAttrName(), "abbrName")) {
                    appModuleVo.setAbbrName(String.valueOf(attrEntityVo.getValueList().get(0)));
                    continue;
                }
                //状态
                if (StringUtils.equals(attrEntityVo.getAttrName(), "state")) {
                    JSONArray statusIdList = attrEntityVo.getValueList();
                    JSONArray statusValueList = attrEntityVo.getActualValueList();
                    if (CollectionUtils.isNotEmpty(statusIdList)) {
                        List<DeployAppUsedStateVo> statusVoList = new ArrayList<>();
                        for (int i = 0; i < statusIdList.size(); i++) {
                            Object id = statusIdList.get(i);
                            Object name = statusValueList.get(i);
                            if (id != null && name != null) {
                                statusVoList.add(new DeployAppUsedStateVo(Long.valueOf(String.valueOf(id)), String.valueOf(name)));
                            }
                        }
                        appModuleVo.setStateList(statusVoList);
                    }
                    continue;
                }
                //负责人
                if (StringUtils.equals(attrEntityVo.getAttrName(), "owner")) {
                    JSONArray stateIdList = attrEntityVo.getValueList();
                    JSONArray statusValueList = attrEntityVo.getActualValueList();
                    if (CollectionUtils.isNotEmpty(stateIdList)) {
                        List<DeployAppOwnerVo> ownerVoList = new ArrayList<>();
                        for (int i = 0; i < stateIdList.size(); i++) {
                            Object id = stateIdList.get(i);
                            Object name = statusValueList.get(i);
                            if (id != null && name != null) {
                                ownerVoList.add(new DeployAppOwnerVo(Long.valueOf(String.valueOf(id)), String.valueOf(name)));
                            }
                        }
                        appModuleVo.setOwnerList(ownerVoList);
                    }
                    continue;
                }
                //维窗口
                if (StringUtils.equals(attrEntityVo.getAttrName(), "maintenance_window")) {
                    appModuleVo.setMaintenanceWindow(String.valueOf(attrEntityVo.getValueList().get(0)));
                    continue;
                }
                //备注
                if (StringUtils.equals(attrEntityVo.getAttrName(), "description")) {
                    appModuleVo.setDescription(String.valueOf(attrEntityVo.getValueList().get(0)));
                }
            }
        }
        return appModuleVo;
    }
}
