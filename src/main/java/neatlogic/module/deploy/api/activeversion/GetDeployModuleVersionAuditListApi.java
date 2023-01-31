package neatlogic.module.deploy.api.activeversion;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployEnvVersionMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployModuleVersionAuditListApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Override
    public String getName() {
        return "获取模块版本历史";
    }

    @Override
    public String getToken() {
        return "deploy/module/version/audit/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
    })
    @Output({
    })
    @Description(desc = "获取模块版本历史")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        List<DeployEnvVersionAuditVo> auditList = deployEnvVersionMapper.getDeployEnvVersionAuditBySystemIdAndModueIdAndEnvId(appSystemId, appModuleId, envId);
        if (auditList.size() > 0) {
            Set<Long> versionIdSet = auditList.stream().map(DeployEnvVersionAuditVo::getNewVersionId).collect(Collectors.toSet());
            versionIdSet.addAll(auditList.stream().map(DeployEnvVersionAuditVo::getOldVersionId).collect(Collectors.toSet()));
            List<DeployVersionVo> versionList = deployVersionMapper.getDeployVersionBaseInfoByIdList(new ArrayList<>(versionIdSet));
            Map<Long, String> versionMap = versionList.stream().collect(Collectors.toMap(DeployVersionVo::getId, DeployVersionVo::getVersion));
            for (DeployEnvVersionAuditVo vo : auditList) {
                vo.setNewVersion(versionMap.get(vo.getNewVersionId()));
                vo.setOldVersion(versionMap.get(vo.getOldVersionId()));
            }
        }
        return auditList;
    }
}
