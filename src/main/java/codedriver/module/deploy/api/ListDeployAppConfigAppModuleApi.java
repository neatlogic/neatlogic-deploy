package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
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
import java.util.Objects;
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "发布应用配置的应用系统模块列表")
    })
    @Description(desc = "查询发布应用配置的应用系统模块列表(树的模块下拉、一键发布页面根据系统环境查询模块列表)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<CiEntityVo> moduleCiEntityList = new ArrayList<>();
        List<DeployAppModuleVo> returnAppModuleVoList = new ArrayList<>();

        if (Objects.isNull(paramObj.getLong("envId"))) {
            //查询系统下模块列表
            //TODO 补模块简称、考虑权限问题 优化为动态sql时补充
            List<Long> idList = resourceCenterMapper.getAppSystemModuleIdListByAppSystemId(paramObj.getLong("appSystemId"), TenantContext.get().getDataDbName());
            if (CollectionUtils.isNotEmpty(idList)) {
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                moduleCiEntityList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(idList);
            }

            //补充模块是否有环境（有实例的环境）
            List<Long> hasEnvAppModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), moduleCiEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
            for (CiEntityVo ciEntityVo : moduleCiEntityList) {
                DeployAppModuleVo returnAppModuleVo = new DeployAppModuleVo(ciEntityVo.getId(), ciEntityVo.getName());
                returnAppModuleVoList.add(returnAppModuleVo);
                if (hasEnvAppModuleIdList.contains(ciEntityVo.getId())) {
                    returnAppModuleVo.setIsHasEnv(1);
                }
            }
        } else {
            int count = deployAppConfigMapper.getAppModuleCountBySystemIdAndEnvId(paramObj.getLong("appSystemId"),paramObj.getLong("envId"),TenantContext.get().getDataDbName());
            if (count > 0) {
                returnAppModuleVoList = deployAppConfigMapper.getAppModuleListBySystemIdAndEnvId(paramObj.getLong("appSystemId"), paramObj.getLong("envId"),TenantContext.get().getDataDbName());
            }
        }
        return returnAppModuleVoList;
    }
}
