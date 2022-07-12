package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
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
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAppModuleEnvAutoConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            instanceList = resourceCenterMapper.getResourceByIdList(defaultValue.toJavaList(Long.class), TenantContext.get().getDataDbName());
        } else {
            int count = deployAppConfigMapper.getAppModuleEnvAutoConfigInstanceIdCount(searchVo, TenantContext.get().getDataDbName());
            if (count > 0) {
                searchVo.setRowNum(count);
                List<Long> instanceIdList = deployAppConfigMapper.getAppModuleEnvAutoConfigInstanceIdList(searchVo, TenantContext.get().getDataDbName());
                if (CollectionUtils.isNotEmpty(instanceIdList)) {
                    instanceList = resourceCenterMapper.getResourceByIdList(instanceIdList, TenantContext.get().getDataDbName());
                }
            }
        }
        return TableResultUtil.getResult(instanceList, searchVo);
    }
}
