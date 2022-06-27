package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
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

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        List<DeployAppConfigResourceVo> returnAppSystemList = new ArrayList<>();
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        Integer count = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);

            //查询包含关键字的 returnAppSystemList
            List<Long> appSystemIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
            if (CollectionUtils.isEmpty(appSystemIdList)) {
                return TableResultUtil.getResult(returnAppSystemList, searchVo);
            }
            returnAppSystemList = deployAppConfigMapper.getAppSystemListByIdList(appSystemIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            Map<Long, DeployAppConfigResourceVo> returnAppSystemMap = returnAppSystemList.stream().collect(Collectors.toMap(DeployAppConfigResourceVo::getAppSystemId, e -> e));

            //补充系统是否有模块
            List<Long> hasModuleAppSystemIdList = resourceCenterMapper.getAppModuleIdListByAppSystemIdList(returnAppSystemList.stream().map(DeployAppConfigResourceVo::getAppSystemId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
            for (DeployAppConfigResourceVo resourceVo : returnAppSystemList) {
                if (hasModuleAppSystemIdList.contains(resourceVo.getAppSystemId())) {
                    resourceVo.setIsHasModule(1);
                }
            }

            //查询包含关键字的 appSystemModuleList，再将信息模块信息补回 returnAppSystemList
            List<Long> appSystemIdListByAppModuleName = appSystemMapper.getAppSystemIdListByAppModuleName(searchVo.getKeyword(), TenantContext.get().getDataDbName());
            List<DeployAppConfigResourceVo> appSystemModuleList = deployAppConfigMapper.getAppSystemModuleListBySystemIdList(appSystemIdListByAppModuleName, paramObj.getInteger("isConfig"), TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            if (CollectionUtils.isNotEmpty(appSystemModuleList)) {
                for (DeployAppConfigResourceVo appSystemInfoVo : appSystemModuleList) {
                    DeployAppConfigResourceVo returnAppSystemVo = returnAppSystemMap.get(appSystemInfoVo.getAppSystemId());
                    if (returnAppSystemVo != null) {

                        //补充模块是否有环境（有实例的环境）
                        List<Long> appModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(returnAppSystemVo.getAppSystemId(),appSystemInfoVo.getAppModuleList().stream().map(DeployAppModuleVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
                        for (DeployAppModuleVo appModuleVo : appSystemInfoVo.getAppModuleList()) {
                            if (appModuleIdList.contains(appModuleVo.getId())) {
                                appModuleVo.setIsHasEnv(1);
                            }
                        }
                        //补充系统下的模块列表
                        returnAppSystemVo.setAppModuleList(appSystemInfoVo.getAppModuleList());
                    }
                }
            }
        }
        return TableResultUtil.getResult(returnAppSystemList, searchVo);
    }
}


