package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/23 4:32 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppModuleEnvAutoConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/module/env/auto/config/instance/search";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.keyword"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.envid"),
            @Param(name = "isAutoConfig", type = ApiParamType.INTEGER, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.isautoconfig"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.defaultvalue"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.input.param.desc.needpage")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "nmdaae.searchdeployappmoduleenvautoconfiginstanceapi.output.param.desc.tbodylist")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppEnvAutoConfigVo searchVo = paramObj.toJavaObject(DeployAppEnvAutoConfigVo.class);
        List<ResourceVo> instanceList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdListSimple(defaultValue.toJavaList(Long.class));
        } else {
            int count = deployAppConfigMapper.getAppModuleEnvAutoConfigInstanceIdCount(searchVo);
            if (count > 0) {
                searchVo.setRowNum(count);
                List<Long> instanceIdList = deployAppConfigMapper.getAppModuleEnvAutoConfigInstanceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(instanceIdList)) {
                    instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdListSimple(instanceIdList);
                }
            }
        }
        return TableResultUtil.getResult(instanceList, searchVo);
    }
}
