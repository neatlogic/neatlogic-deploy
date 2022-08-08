package codedriver.module.deploy.api.activeversion;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppModuleEnvVo;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployActiveVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布活动版本";
    }

    @Override
    public String getToken() {
        return "deploy/activeversion/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认应用系统ID列表(为空则代表查看所有应用)"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
    })
    @Output({
    })
    @Description(desc = "查询发布活动版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        TenantContext.get().switchDataDatabase();
        // 按系统分页
        Integer systemIdListCount = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
        if (systemIdListCount > 0) {
            List<Long> systemIdList = deployAppConfigMapper.searchAppSystemIdList(searchVo);
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            // 查询每个系统的模块
            for (Long systemId : systemIdList) {
                TenantContext.get().switchDataDatabase();
                List<Long> moduleIdList = resourceCrossoverMapper.getAppSystemModuleIdListByAppSystemId(systemId);
                TenantContext.get().switchDefaultDatabase();
                if (moduleIdList.size() > 0) {
                    // // todo 找出所有模块各自的所有版本
                    // 查询当前系统所有模块各自所拥有的环境
                    // todo 模块没有环境要不要查出来？
                    List<DeployAppModuleEnvVo> moduleEnvList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemId(systemId, TenantContext.get().getDataDbName());
                    Map<Long, List<AppEnvironmentVo>> moduleEnvListMap;
                    if (moduleEnvList.size() > 0) {
                        moduleEnvListMap = moduleEnvList.stream().collect(Collectors.toMap(DeployAppModuleEnvVo::getId, DeployAppModuleEnvVo::getEnvList));
                        // 查出每个模块下的活动版本与最新非活动版本
                        // 活动版本：没有走到最后一个环境的版本
                        // 最新非活动版本：PRD环境（或自定义的顺序最后的环境）的当前版本是谁，谁就是最新的非活动版本
                        for (Long moduleId : moduleIdList) {
                            // todo 以环境为角度，取出当前模块下的所有环境（每个环境的记录按fcd排序），
                            //  然后用版本作为外层循环，找出每个环境中找出当前版本的最后一次出现的位置，
                            //  如果记录是【最后一条】或【最后一条记录的版本大于当前版本】，则认为当前版本在当前环境发布了，
                            //  如果记录不是最后一条或最后一条记录的版本小于当前版本，则认为当前版本在当前环境回退过，回退时间为下一条记录的fcd
                            //  版本在所有环境都被认为发布了，即可称之为非活动版本
                        }
                    }

                }
            }

        }

        return null;
    }
}
