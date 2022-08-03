package codedriver.module.deploy.api.version;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.version.DeployVersionEnvInstanceVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployVersionInstanceApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "查询版本实例发布状态";
    }

    @Override
    public String getToken() {
        return "deploy/version/instance/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键词"),
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id")
    })
    @Output({
            @Param(name = "Return", explode = DeployVersionEnvInstanceVo[].class),
    })
    @Description(desc = "查询版本实例发布状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        Long versionId = paramObj.getLong("versionId");
        Long envId = paramObj.getLong("envId");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoById(versionId);
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        List<DeployVersionEnvInstanceVo> result = new ArrayList<>();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<Long> instanceIdList = resourceCrossoverMapper.getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(versionVo.getAppSystemId(), versionVo.getAppModuleId(), envId), TenantContext.get().getDataDbName());
        if (instanceIdList.size() > 0) {
            List<DeployVersionEnvInstanceVo> deployedInstanceList = deployVersionMapper.getDeployedInstanceByVersionIdAndEnvId(versionId, envId);
            List<ResourceVo> instanceList = resourceCrossoverMapper.getAppInstanceResourceListByIdListAndKeyword(instanceIdList, keyword, TenantContext.get().getDataDbName());
            for (ResourceVo ins : instanceList) {
                DeployVersionEnvInstanceVo vo = new DeployVersionEnvInstanceVo(ins.getId(), ins.getName(), ins.getIp());
                Optional<DeployVersionEnvInstanceVo> first = deployedInstanceList.stream().filter(o -> Objects.equals(o.getResourceId(), ins.getId())).findFirst();
                if (first.isPresent()) {
                    DeployVersionEnvInstanceVo deployedInstanceVo = first.get();
                    vo.setDeployUser(deployedInstanceVo.getDeployUser());
                    vo.setDeployTime(deployedInstanceVo.getDeployTime());
                    vo.setStatus(1);
                    vo.setDeployUserVo(deployedInstanceVo.getDeployUserVo());
                }
                result.add(vo);
            }
        }
        return result;
    }
}
