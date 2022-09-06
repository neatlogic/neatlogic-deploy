package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
            List<Long> idList = deployCiMapper.searchDeployCiIdList(deployCiVo);
            if (idList.size() > 0) {
                list = deployCiMapper.getDeployCiListByIdList(idList);
            }
        }
        return TableResultUtil.getResult(list, deployCiVo);
    }

}
