package codedriver.module.deploy.service;

import codedriver.framework.autoexec.exception.AutoexecJobRunnerGroupRunnerNotFoundException;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeployVersionServiceImp implements DeployVersionService {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version, String envName) {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        if (buildNo == null && envId == null) {
            throw new ParamNotExistsException("buildNo", "envId");
        }
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(version.getAppSystemId());
        if (appSystemEntity == null) {
            throw new CiEntityNotFoundException(version.getAppSystemId());
        }
        CiEntityVo appModuleEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(version.getAppModuleId());
        if (appModuleEntity == null) {
            throw new CiEntityNotFoundException(version.getAppModuleId());
        }
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(version.getAppSystemId(), version.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystemEntity.getName() + "(" + version.getAppSystemId() + ")", appModuleEntity.getName() + "(" + version.getAppModuleId() + ")");
        }
        List<RunnerMapVo> runnerMapList = runnerGroupVo.getRunnerMapList();
        if (CollectionUtils.isEmpty(runnerMapList)) {
            throw new AutoexecJobRunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        String url;
        if (buildNo != null) {
            DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(id, buildNo);
            if (buildNoVo == null) {
                throw new DeployVersionBuildNoNotFoundException(buildNo);
            }
            url = getRunnerUrl(runnerMapList, buildNoVo.getRunnerMapId(), buildNoVo.getRunnerGroup(), version.getVersion(), buildNo, null);
            if (url == null) {
                throw new DeployVersionRunnerNotFoundException(version.getVersion(), buildNo);
            }
        } else {
            DeployVersionEnvVo envVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(id, envId);
            if (envVo == null) {
                throw new DeployVersionEnvNotFoundException(envId);
            }
            url = getRunnerUrl(runnerMapList, envVo.getRunnerMapId(), envVo.getRunnerGroup(), version.getVersion(), null, envName);
            if (url == null) {
                throw new DeployVersionRunnerNotFoundException(version.getVersion(), envName);
            }
        }
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

    /**
     * 查询builNo或env的runner
     * 如果在应用模块配置的{runnerMapList}中找到buildNo或env记录的{runnerMapId}，则获取此runner url
     * 否则尝试取{runnerMapList}与{runnerGroup}交集中的任一runner
     *
     * @param runnerMapList 应用模块配置runner
     * @param runnerMapId   buildNo或env记录的runner
     * @param runnerGroup   buildNo或env记录的runnerGroup
     * @param version       版本号
     * @param buildNo       buildNo
     * @param envName       环境名
     * @return
     */
    private String getRunnerUrl(List<RunnerMapVo> runnerMapList, Long runnerMapId, JSONObject runnerGroup, String version, Integer buildNo, String envName) {
        String url = null;
        Optional<RunnerMapVo> first = runnerMapList.stream().filter(o -> Objects.equals(o.getRunnerMapId(), runnerMapId)).findFirst();
        if (first.isPresent()) {
            url = first.get().getUrl();
        } else {
            if (MapUtils.isEmpty(runnerGroup)) {
                if (buildNo != null) {
                    throw new DeployVersionRunnerNotFoundException(version, buildNo);
                } else {
                    throw new DeployVersionRunnerNotFoundException(version, envName);
                }
            }
            List<Long> buildNoRunnerMapIdList = runnerGroup.keySet().stream().map(Long::valueOf).collect(Collectors.toList());
            for (RunnerMapVo mapVo : runnerMapList) {
                if (buildNoRunnerMapIdList.contains(mapVo.getRunnerMapId())) {
                    url = mapVo.getUrl();
                    break;
                }
            }
        }
        return url;
    }
}
