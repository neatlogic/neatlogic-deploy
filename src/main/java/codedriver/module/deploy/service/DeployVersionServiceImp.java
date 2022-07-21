package codedriver.module.deploy.service;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployJobNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionBuildNoNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionResourceHasBeenLockedException;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DeployVersionServiceImp implements DeployVersionService {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Override
    public String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version, String envName) {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        if (buildNo == null && envId == null) {
            throw new ParamNotExistsException("buildNo", "envId");
        }
        Long runnerMapId;
        if (buildNo != null) {
            DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(id, buildNo);
            if (buildNoVo == null) {
                throw new DeployVersionBuildNoNotFoundException(buildNo);
            }
            runnerMapId = buildNoVo.getRunnerMapId();
        } else {
            DeployVersionEnvVo envVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(id, envId);
            if (envVo == null) {
                throw new DeployVersionEnvNotFoundException(envId);
            }
            runnerMapId = envVo.getRunnerMapId();
        }
        RunnerMapVo runner = runnerMapper.getRunnerMapByRunnerMapId(runnerMapId);
        if (runner == null) {
            throw new RunnerNotFoundByRunnerMapIdException(runnerMapId);
        }
        String url = runner.getUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    @Override
    public String getVersionResourceFullPath(DeployVersionVo version, DeployResourceType resourceType, Integer buildNo, String envName, String customPath) {
        return getVersionResourceHomePath(version, resourceType, buildNo, envName) + (customPath.startsWith("/") ? customPath : "/" + customPath);
    }

    @Override
    public String getVersionResourceHomePath(DeployVersionVo version, DeployResourceType resourceType, Integer buildNo, String envName) {
        StringBuilder path = new StringBuilder();
        path.append(version.getAppSystemId()).append("/").append(version.getAppModuleId()).append("/").append("artifact/");
        if (resourceType.getValue().startsWith("build")) {
            path.append(version.getVersion()).append("/")
                    .append("build/").append(buildNo).append("/")
                    .append(resourceType.getDirectoryName());
        } else if (resourceType.getValue().startsWith("env")) {
            path.append(version.getVersion()).append("/")
                    .append("env/").append(envName).append("/")
                    .append(resourceType.getDirectoryName());
        } else if (resourceType.getValue().startsWith("mirror")) {
            path.append("mirror/").append(envName).append("/").append(resourceType.getDirectoryName());
        }
        return path.toString();
    }

    @Override
    public String getWorkspaceRunnerUrl(Long appSystemId, Long appModuleId) {
        if (appSystemId == null) {
            throw new ParamNotExistsException("appSystemId");
        }
        if (appModuleId == null) {
            throw new ParamNotExistsException("appModuleId");
        }
        Long runnerMapId = deployJobMapper.getRecentlyJobRunnerMapIdByAppSystemIdAndAppModuleId(appSystemId, appModuleId);
        if (runnerMapId == null) {
            ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            String appName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(appSystemId);
            String moduleName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(appModuleId);
            throw new DeployJobNotFoundException(appName != null ? appName : appSystemId.toString(), moduleName != null ? moduleName : appModuleId.toString());
        }
        RunnerMapVo runner = runnerMapper.getRunnerMapByRunnerMapId(runnerMapId);
        if (runner == null) {
            throw new RunnerNotFoundByRunnerMapIdException(runnerMapId);
        }
        String url = runner.getUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    @Override
    public String getWorkspaceResourceHomePath(Long appSystemId, Long appModuleId) {
        return getWorkspaceResourceFullPath(appSystemId, appModuleId, null);
    }

    @Override
    public String getWorkspaceResourceFullPath(Long appSystemId, Long appModuleId, String customPath) {
        String path = appSystemId + "/" + appModuleId + "/" + DeployResourceType.WORKSPACE.getDirectoryName();
        if (StringUtils.isNotBlank(customPath)) {
            path += (customPath.startsWith("/") ? customPath : "/" + customPath);
        }
        return path;
    }

    @Override
    public void checkHomeHasBeenLocked(String runnerUrl, String path) {
        IGlobalLockHandler handler = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue());
        JSONObject lockJson = new JSONObject();
        lockJson.put("runnerUrl", runnerUrl);
        lockJson.put("path", path);
        if (handler.getIsBeenLocked(lockJson)) {
            throw new DeployVersionResourceHasBeenLockedException();
        }
    }
}
