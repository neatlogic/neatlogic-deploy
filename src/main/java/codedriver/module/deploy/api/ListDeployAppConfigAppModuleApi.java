package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/24 10:09 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统模块列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/module/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.JSONARRAY, isRequired = true, desc = "应用系统id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "查询发布应用配置的应用系统模块列表")
    })
    @Description(desc = "查询发布应用配置的应用系统模块列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppModuleVo searchVo = paramObj.toJavaObject(DeployAppModuleVo.class);
        List<ResourceVo> resourceVoList = new ArrayList<>();
        List<DeployAppModuleVo> returnAppModuleVoList = new ArrayList<>();
        //查询系统下模块列表
        List<Long> idList = resourceCenterMapper.getAppSystemModuleIdListByAppSystemId(paramObj.getLong("appSystemId"), TenantContext.get().getDataDbName());
        if (CollectionUtils.isNotEmpty(idList)) {
            int rowNum = idList.size();
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                int fromIndex = searchVo.getStartNum();
                int toIndex = fromIndex + searchVo.getPageSize();
                toIndex = Math.min(toIndex, rowNum);
                idList.sort(Comparator.reverseOrder());
                List<Long> currentPageIdList = idList.subList(fromIndex, toIndex);
                if (CollectionUtils.isNotEmpty(currentPageIdList)) {
                    resourceVoList = resourceCenterMapper.getAppModuleListByIdList(currentPageIdList, TenantContext.get().getDataDbName());
                }
            }
        }

        //补充模块是否有环境（有实例的环境）
        List<Long> hasEnvAppModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), resourceVoList.stream().map(ResourceVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
        for (ResourceVo resourceVo : resourceVoList) {
            DeployAppModuleVo returnAppModuleVo = new DeployAppModuleVo(resourceVo.getId(), resourceVo.getName());
            returnAppModuleVoList.add(returnAppModuleVo);
            if (hasEnvAppModuleIdList.contains(resourceVo.getId())) {
                returnAppModuleVo.setIsHasEnv(1);
            }
        }
        return TableResultUtil.getResult(returnAppModuleVoList, searchVo);
    }
}
