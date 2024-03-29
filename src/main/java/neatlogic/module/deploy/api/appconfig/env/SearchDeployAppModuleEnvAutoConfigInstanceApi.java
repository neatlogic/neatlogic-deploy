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
        return "获取发布应用模块环境AutoConfig实例差异的资产列表";
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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
            @Param(name = "isAutoConfig", type = ApiParamType.INTEGER, desc = "是否有AutoConfig"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "发布应用模块环境AutoConfig实例差异的资产列表")
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
