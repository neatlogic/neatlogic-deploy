package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/20 5:13 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/search";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索-应用名|模块名"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigResourceVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（含关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        List<DeployAppConfigResourceVo> returnAppSystemList = new ArrayList<>();
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        Integer count = appSystemMapper.getAppSystemIdListCount(searchVo);
        if (count > 0) {
            //查询包含关键字的 returnAppSystemList
            List<Long> appSystemIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
            returnAppSystemList = deployAppConfigMapper.getAppSystemListByIdList(appSystemIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            Map<Long, DeployAppConfigResourceVo> returnAppSystemMap = returnAppSystemList.stream().collect(Collectors.toMap(DeployAppConfigResourceVo::getAppSystemId, e -> e));
            searchVo.setRowNum(count);

            //查询包含关键字的 appSystemModuleList，再将信息模块信息补回 returnAppSystemList
            List<Long> appSystemIdListByAppModuleName = appSystemMapper.getAppSystemIdListByAppModuleName(searchVo.getKeyword(), TenantContext.get().getDataDbName());
            List<DeployAppConfigResourceVo> appSystemModuleList = deployAppConfigMapper.getAppSystemModuleListBySystemIdList(appSystemIdListByAppModuleName, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            if (CollectionUtils.isNotEmpty(appSystemModuleList)) {
                for (DeployAppConfigResourceVo appSystemInfoVo : appSystemModuleList) {
                    DeployAppConfigResourceVo returnAppSystemVo = returnAppSystemMap.get(appSystemInfoVo.getAppSystemId());
                    if (returnAppSystemVo != null) {
                        returnAppSystemVo.setAppModuleList(appSystemInfoVo.getAppModuleList());
                    }
                }
            }
        }

        return TableResultUtil.getResult(returnAppSystemList, searchVo);
    }
}


