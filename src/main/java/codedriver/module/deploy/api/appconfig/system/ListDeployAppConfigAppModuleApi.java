package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/24 10:09 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

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
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "发布应用配置的应用系统模块列表")
    })
    @Description(desc = "查询发布应用配置的应用系统模块列表(树的模块下拉)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ResourceVo> moduleResourceList = new ArrayList<>();
        List<DeployAppModuleVo> returnAppModuleVoList = new ArrayList<>();

        //查询系统下模块列表
        //TODO 考虑权限问题
        TenantContext.get().switchDataDatabase();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<Long> moduleIdList = resourceCrossoverMapper.getAppSystemModuleIdListByAppSystemIdAndAppModuleIdList(paramObj.getLong("appSystemId"), paramObj.getJSONArray("appModuleIdList"));
        if (CollectionUtils.isNotEmpty(moduleIdList)) {
            moduleResourceList = resourceCrossoverMapper.getAppModuleListByIdListSimple(moduleIdList);
        }
        TenantContext.get().switchDefaultDatabase();

        int isHasConfig = 0;
        if (CollectionUtils.isNotEmpty(deployAppConfigMapper.getAppConfigListByAppSystemId(paramObj.getLong("appSystemId")))) {
            isHasConfig = 1;
        }

        //补充模块是否有环境（有实例的环境）
        List<Long> hasEnvAppModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), moduleResourceList.stream().map(ResourceVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
        for (ResourceVo resourceVo : moduleResourceList) {
            DeployAppModuleVo returnAppModuleVo = new DeployAppModuleVo(resourceVo.getId(), resourceVo.getName(), resourceVo.getAbbrName());
            returnAppModuleVoList.add(returnAppModuleVo);
            if (hasEnvAppModuleIdList.contains(resourceVo.getId())) {
                returnAppModuleVo.setIsHasEnv(1);
            }
            returnAppModuleVo.setIsConfig(isHasConfig);
        }
        return returnAppModuleVoList;
    }
}
