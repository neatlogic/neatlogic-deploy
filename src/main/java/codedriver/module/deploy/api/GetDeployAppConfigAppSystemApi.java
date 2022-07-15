package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/6/22 11:44 上午
 */
@Service
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
        return "deploy/app/config/app/system/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = CiEntityVo[].class, desc = "应用系统详细配置信息")
    })
    @Description(desc ="查询应用系统（配置项）信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        CiEntityVo ciEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("appSystemId"));

        List<AttrEntityVo> attrEntityList = appSystemInfo.getAttrEntityList();
//        if (MapUtils.isNotEmpty(attrEntityData)) {
//
//        }


        JSONObject appSystemInfoObject = new JSONObject();
        appSystemInfoObject.put("appSystemInfo", appSystemInfo);
        return appSystemInfoObject;
    }
}
