package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/6/22 11:44 上午
 */
@Service
@AuthAction(action = DEPLOY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigAppSystemInfoApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "查询应用系统详细配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/app/system/info/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = CiEntityVo[].class, desc = "应用系统详细配置信息")
    })
    @Description(desc = "查询应用系统详细配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }

        CiEntityVo ciEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("appSystemId"));

        JSONObject appSystemInfoObject = new JSONObject();
        appSystemInfoObject.put("appSystemInfo", appSystemInfo);
        return appSystemInfoObject;
    }
}
