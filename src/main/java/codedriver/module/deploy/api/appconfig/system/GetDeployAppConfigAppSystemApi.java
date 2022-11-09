package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppSystemVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/6/22 11:44 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "查询应用系统（配置项）信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/get";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(explode = CiEntityVo[].class, desc = "应用系统（配置项）信息")
    })
    @Description(desc = "查询应用系统（配置项）信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取应用系统ciEntityVo
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("id"));
        DeployAppSystemVo appSystemVo = new DeployAppSystemVo();
        if (ciEntityVo == null) {
            return appSystemVo;
        }
        //获取属性
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("id"));
        appSystemVo.setId(appSystemInfo.getId());
        List<AttrEntityVo> attrEntityList = appSystemInfo.getAttrEntityList();
        if (CollectionUtils.isNotEmpty(attrEntityList)) {
            for (AttrEntityVo attrEntityVo : attrEntityList) {

                //名称
                if (StringUtils.equals(attrEntityVo.getAttrName(), "name")) {
                    appSystemVo.setName(String.valueOf(attrEntityVo.getValueList().get(0)));
                    continue;
                }
                //简称
                if (StringUtils.equals(attrEntityVo.getAttrName(), "abbrName")) {
                    appSystemVo.setAbbrName(String.valueOf(attrEntityVo.getValueList().get(0)));
                    continue;
                }
                //状态
                if (StringUtils.equals(attrEntityVo.getAttrName(), "state")) {
                    JSONArray stateIdArray = attrEntityVo.getValueList();
                    if (CollectionUtils.isNotEmpty(stateIdArray)) {
                        List<Long> stateIdList = stateIdArray.toJavaList(Long.class);
                        appSystemVo.setState(stateIdList);
                    }
                }
                //负责人
                if (StringUtils.equals(attrEntityVo.getAttrName(), "owner")) {
                    JSONArray ownerIdArray = attrEntityVo.getValueList();
                    if (CollectionUtils.isNotEmpty(ownerIdArray)) {
                        List<Long> ownerIdList = ownerIdArray.toJavaList(Long.class);
                        appSystemVo.setOwner(ownerIdList);
                    }
                }
                //维护窗口
                if (StringUtils.equals(attrEntityVo.getAttrName(), "maintenance_window")) {
                    JSONArray mainWindowArray = attrEntityVo.getValueList();
                    if (CollectionUtils.isNotEmpty(mainWindowArray)) {
                        List<String> mainWindowList = mainWindowArray.toJavaList(String.class);
                        appSystemVo.setMaintenanceWindow(mainWindowList);
                    }
                    continue;
                }
                //备注
                if (StringUtils.equals(attrEntityVo.getAttrName(), "description")) {
                    appSystemVo.setDescription(String.valueOf(attrEntityVo.getValueList().get(0)));
                }
            }
        }
        return appSystemVo;
    }
}
