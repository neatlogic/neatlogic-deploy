package neatlogic.module.deploy.api.version.dependency;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.DeployPackageVo;
import neatlogic.framework.deploy.dto.version.DeployVersionDependencyVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployPackageNotFoundException;
import neatlogic.framework.deploy.exception.DeployPackageRequiredAttributeLostException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionParentDependencyNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPackageMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployVersionDependencyForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployPackageMapper deployPackageMapper;

    @Override
    public String getName() {
        return "nmdavd.savedeployversiondependencyforautoexecapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/dependency/save/forautoexec";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.sysid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.moduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.version", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "dependenceList", type = ApiParamType.JSONARRAY, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.name", isRequired = true),
            @Param(name = "dependenceList.groupId", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.groupid"),
            @Param(name = "dependenceList.artifactId", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.artifactid"),
            @Param(name = "dependenceList.version", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.version"),
            @Param(name = "dependenceList.licenses", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.licenses"),
            @Param(name = "dependenceList.scope", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.scope"),
            @Param(name = "dependenceList.type", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.type"),
            @Param(name = "dependenceList.url", type = ApiParamType.STRING, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.url"),
            @Param(name = "dependenceList.parent", type = ApiParamType.JSONOBJECT, desc = "nmdavd.savedeployversiondependencyforautoexecapi.input.param.desc.dependencelist.parent"),
    })
    @Output({
    })
    @Description(desc = "nmdavd.savedeployversiondependencyforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        JSONArray dependenceList = paramObj.getJSONArray("dependenceList");
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        // 以当前请求的dependenceList为准，新增数据库中不存在的依赖、删除dependenceList中不存在的依赖，更新两者均存在的依赖的build_time
        List<DeployVersionDependencyVo> versionDependencyList = deployVersionMapper.getDeployVersionDependencyListByVersionId(versionVo.getId());
        for (int i = 0; i < dependenceList.size(); i++) {
            JSONObject object = dependenceList.getJSONObject(i);
            String groupId = object.getString("groupId");
            String artifactId = object.getString("artifactId");
            String pkgVersion = object.getString("version");
            String license = object.getString("license");
            String url = object.getString("url");
            String type = object.getString("type");
            String scope = object.getString("scope");
            if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId) || StringUtils.isBlank(pkgVersion)) {
                throw new DeployPackageRequiredAttributeLostException(i);
            }
            JSONObject parent = object.getJSONObject("parent");
            DeployPackageVo pkg = deployPackageMapper.getPackageByGroupIdAndArtifactIdAndVersion(groupId, artifactId, pkgVersion);
            if (pkg == null) {
                pkg = new DeployPackageVo(groupId, artifactId, pkgVersion, license, url, type);
                deployPackageMapper.insertPackage(pkg);
            }
            Long parentDependencyId = null;
            if (MapUtils.isNotEmpty(parent)) {
                String parentGroupId = parent.getString("groupId");
                String parentArtifactId = parent.getString("artifactId");
                String parentVersion = parent.getString("version");
                DeployPackageVo parentPkg = deployPackageMapper.getPackageByGroupIdAndArtifactIdAndVersion(parentGroupId, parentArtifactId, parentVersion);
                if (parentPkg == null) {
                    throw new DeployPackageNotFoundException(parentGroupId, parentArtifactId, parentVersion);
                }
                DeployVersionDependencyVo parentDependency = deployVersionMapper.getDeployVersionDependencyByVersionIdAndPackageId(versionVo.getId(), parentPkg.getId());
                if (parentDependency == null) {
                    throw new DeployVersionParentDependencyNotFoundException(parentPkg);
                }
                parentDependencyId = parentDependency.getId();
            }
            DeployVersionDependencyVo dependencyVo = new DeployVersionDependencyVo(versionVo.getId(), pkg.getId(), scope, parentDependencyId);
            versionDependencyList.remove(dependencyVo);
            DeployVersionDependencyVo oldDependencyVo = deployVersionMapper.getDeployVersionDependencyByVersionIdAndPackageId(dependencyVo.getVersionId(), dependencyVo.getPackageId());
            if (oldDependencyVo != null) {
                deployVersionMapper.updateDeployVersionDependencyBuildTimeById(oldDependencyVo.getId());
            } else {
                deployVersionMapper.insertDeployVersionDependency(dependencyVo);
            }
        }
        if (versionDependencyList.size() > 0) {
            deployVersionMapper.deleteDeployVersionDependencyByVersionIdAndPackageIdList(versionVo.getId(), versionDependencyList.stream().map(DeployVersionDependencyVo::getPackageId).collect(Collectors.toList()));
        }
        return null;
    }
}
