package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployCiApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getName() {
        return "查询持续集成配置";
    }

    @Override
    public String getToken() {
        return "deploy/ci/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", desc = "系统ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "keyword", desc = "关键词", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Description(desc = "查询持续集成配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiVo deployCiVo = paramObj.toJavaObject(DeployCiVo.class);
        int count = deployCiMapper.searchDeployCiCount(deployCiVo);
        deployCiVo.setRowNum(count);
        List<DeployCiVo> list = new ArrayList<>();
        if (count > 0) {
            list = deployCiMapper.searchDeployCiList(deployCiVo);
            if (list.size() > 0) {
                List<Long> moduleIdList = list.stream().map(DeployCiVo::getAppModuleId).collect(Collectors.toList());
                IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
                List<ResourceVo> moduleList = resourceCrossoverMapper.getAppModuleListByIdListSimple(moduleIdList, false);
                if (moduleList.size() > 0) {
                    Map<Long, String> moduleMap = moduleList.stream().collect(Collectors.toMap(ResourceVo::getId, ResourceVo::getAbbrName));
                    list.forEach(o -> o.setAppModuleAbbrName(moduleMap.get(o.getAppModuleId())));
                }
            }
        }
        return TableResultUtil.getResult(list, deployCiVo);
    }

}
