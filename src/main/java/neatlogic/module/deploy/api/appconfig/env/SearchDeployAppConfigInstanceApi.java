package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigInstanceVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/29 3:44 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.searchdeployappconfiginstanceapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.keyword"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.envid"),
            @Param(name = "currentPage", desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.needpage", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "nmdaae.searchdeployappconfiginstanceapi.input.param.desc.pagesize", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "nmdaae.searchdeployappconfiginstanceapi.output.param.desc.tbodylist"),
    })
    @Description(desc = "nmdaae.searchdeployappconfiginstanceapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigInstanceVo searchVo = paramObj.toJavaObject(DeployAppConfigInstanceVo.class);
        List<DeployAppConfigInstanceVo> instanceList = new ArrayList<>();
        int count = deployAppConfigMapper.getAppConfigEnvInstanceCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> instanceIdList = deployAppConfigMapper.searchAppConfigEnvInstanceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(instanceIdList)) {
                instanceList = deployAppConfigMapper.searchAppConfigEnvInstanceListByIdList(instanceIdList);
            }
        }
        return TableResultUtil.getResult(instanceList, searchVo);
    }
}
