package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.deploy.exception.verison.DeployVersionSyncFailedException;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.globallock.core.GlobalLockHandlerFactory;
import neatlogic.framework.globallock.core.IGlobalLockHandler;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeployVersionServiceImpl implements DeployVersionService {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppConfigService deployAppConfigService;

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
        List<RunnerMapVo> runnerMapList = deployAppConfigService.getAppModuleRunnerGroupByAppSystemIdAndModuleId(version.getAppSystemId(), version.getAppModuleId());
        String url;
        if (buildNo != null) {
            DeployVersionBuildNoVo buildNoVo = deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(id, buildNo);
            if (buildNoVo == null) {
                throw new DeployVersionBuildNoNotFoundException(buildNo);
            }
            url = getRunnerUrl(runnerMapList, buildNoVo.getRunnerMapId(), buildNoVo.getRunnerGroup());
            if (StringUtils.isBlank(url)) {
                throw new DeployVersionRunnerNotFoundException(version.getVersion(), buildNo);
            }
        } else {
            DeployVersionEnvVo envVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(id, envId);
            if (envVo == null) {
                throw new DeployVersionEnvNotFoundException(envId);
            }
            url = getRunnerUrl(runnerMapList, envVo.getRunnerMapId(), envVo.getRunnerGroup());
            if (StringUtils.isBlank(url)) {
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
    public String getWorkspaceRunnerUrl(DeployVersionVo versionVo) {
        String url;
        Long versionRunnerMapId = versionVo.getRunnerMapId();
        JSONObject versionRunnerGroup = versionVo.getRunnerGroup();
        if (versionRunnerMapId == null && MapUtils.isEmpty(versionRunnerGroup)) {
            throw new DeployVersionRunnerNotFoundException(versionVo.getVersion());
        }
        List<RunnerMapVo> runnerMapList = deployAppConfigService.getAppModuleRunnerGroupByAppSystemIdAndModuleId(versionVo.getAppSystemId(), versionVo.getAppModuleId());
        url = getRunnerUrl(runnerMapList, versionRunnerMapId, versionRunnerGroup);
        if (StringUtils.isBlank(url)) {
            throw new DeployVersionRunnerNotFoundException(versionVo.getVersion());
        }
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

    @Override
    public String getEnvName(String version, Long envId) {
        String envName = null;
        if (envId != null) {
            ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
            envName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(envId);
            if (StringUtils.isBlank(envName)) {
                throw new DeployVersionEnvNotFoundException(version, envId);
            }
        }
        return envName;
    }

    /**
     * 查询builNo|env|工程目录的runner
     * 如果在应用模块配置的{runnerMapList}中找到buildNo|env|版本记录的{runnerMapId}，则获取此runner url
     * 否则尝试取{runnerMapList}与{runnerGroup}交集中的任一runner
     *
     * @param runnerMapList 应用模块配置runner
     * @param runnerMapId   buildNo或env记录的runner
     * @param runnerGroup   buildNo或env记录的runnerGroup
     * @return
     */
    private String getRunnerUrl(List<RunnerMapVo> runnerMapList, Long runnerMapId, JSONObject runnerGroup) {
        String url = null;
        Optional<RunnerMapVo> first = runnerMapList.stream().filter(o -> Objects.equals(o.getRunnerMapId(), runnerMapId)).findFirst();
        if (first.isPresent()) {
            url = first.get().getUrl();
        } else if (MapUtils.isNotEmpty(runnerGroup)) {
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

    @Override
    public void syncProjectFile(DeployVersionVo version, String runnerUrl, List<String> targetPathList){
        JSONObject param = new JSONObject();
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(version.getAppSystemId(), version.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(version.getAppSystemName() + "(" + version.getAppSystemId() + ")", version.getAppModuleName() + "(" + version.getAppModuleId() + ")");
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        JSONObject runnerMap = new JSONObject();
        for (RunnerMapVo runnerMapVo : runnerGroupVo.getRunnerMapList()) {
            runnerMap.put(runnerMapVo.getRunnerMapId().toString(), runnerMapVo.getHost());
        }
        param.put("runnerGroup",runnerMap);
        param.put("targetPaths", targetPathList);
        param.put("runnerId",version.getRunnerMapId());
        String url = runnerUrl + "api/rest/deploy/dpversync";
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setConnectTimeout(5000).setReadTimeout(10000).setAuthType(AuthenticateType.BUILDIN).setPayload(param.toJSONString()).sendRequest();
        int responseCode = httpRequestUtil.getResponseCode();
        String error = httpRequestUtil.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new DeployVersionSyncFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new DeployVersionSyncFailedException(error);
            }
        }
        JSONObject result = httpRequestUtil.getResultJson();
        if(result.containsKey("Return") && result.getJSONObject("Return").containsKey("msgError")){
            throw new DeployVersionSyncFailedException(result.getJSONObject("Return").getString("msgError"));
        }
    }
}
