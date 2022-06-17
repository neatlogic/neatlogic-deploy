package codedriver.module.deploy.api;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/17 11:33 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppEnvApi extends PrivateApiComponentBase {

    @Resource
    CiEntityMapper ciEntityMapper;

    @Override
    public String getName() {
        return "查找发布配置环境列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id", isRequired = true),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id", isRequired = true),
    })
    @Output({

    })
    @Description(desc = "查找发布配置环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
      List<CiEntityVo> envCiEntityList = ciEntityMapper.getResourceEnvCientityByAppSystemIdAndAppModuleId();
        return null;
    }
}
