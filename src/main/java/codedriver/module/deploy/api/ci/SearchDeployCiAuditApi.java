package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
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
public class SearchDeployCiAuditApi extends PrivateApiComponentBase {

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    private DeployJobMapper deployJobMapper;

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
            @Param(name = "ciId", desc = "持续集成配置id", type = ApiParamType.LONG, isRequired = true),
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
            if (list.size() > 0) {
                List<Long> jobIdList = list.stream().map(DeployCiAuditVo::getJobId).collect(Collectors.toList());
                DeployJobVo jobVo = new DeployJobVo();
                jobVo.setIdList(jobIdList);
                List<DeployJobVo> jobList = deployJobMapper.searchDeployJob(jobVo);
                if (jobList.size() > 0) {
                    Map<Long, String> map = jobList.stream().collect(Collectors.toMap(AutoexecJobVo::getId, DeployJobVo::getName));
                    for (DeployCiAuditVo vo : list) {
                        vo.setJobName(map.get(vo.getJobId()));
                    }
                }
            }
        }
        return TableResultUtil.getResult(list, auditVo);
    }

}
