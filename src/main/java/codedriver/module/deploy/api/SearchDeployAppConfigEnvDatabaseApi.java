package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.IResourceCenterConfigCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/1 12:18 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigEnvDatabaseApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布应用配置DB库下的数据库";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/database/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "isConfig", type = ApiParamType.INTEGER, desc = "是否有DB配置"),
    })
    @Output({
//            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布应用配置DB库下的数据库")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        IResourceCenterConfigCrossoverService resourceCenterConfigCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterConfigCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterConfigCrossoverService.getResourceCenterConfig();
        CiVo databaseCiVo = null;
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            StringUtils.equals(resourceEntityVo.getName(), "resource_database");
            databaseCiVo = resourceEntityVo.getCi();
        }

        int count = deployAppConfigMapper.getDeployDatabaseResourceCount(searchVo, databaseCiVo.getId(), TenantContext.get().getDataDbName());
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> idList = deployAppConfigMapper.getDeployDatabaseResourceIdList(searchVo, databaseCiVo.getId(), TenantContext.get().getDataDbName());
            if (CollectionUtils.isNotEmpty(idList)) {
                return deployAppConfigMapper.getDeployDatabaseResourceList(idList, databaseCiVo.getId(), TenantContext.get().getDataDbName());
            }
        }
        return null;
    }
}


























