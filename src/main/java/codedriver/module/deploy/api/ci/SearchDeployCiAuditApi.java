package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
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
public class SearchDeployCiAuditApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getName() {
        return "查询持续集成触发记录";
    }

    @Override
    public String getToken() {
        return "deploy/ci/audit/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Description(desc = "查询持续集成触发记录")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployCiAuditVo auditVo = paramObj.toJavaObject(DeployCiAuditVo.class);
        int count = deployCiMapper.searchDeployCiAuditCount(auditVo);
        auditVo.setRowNum(count);
        List<DeployCiAuditVo> list = new ArrayList<>();
        if (count > 0) {
            list = deployCiMapper.searchDeployCiAudit(auditVo);
        }
        return TableResultUtil.getResult(list, auditVo);
    }

}
