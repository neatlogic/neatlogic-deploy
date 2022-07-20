package codedriver.module.deploy.api.version.resource;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.core.IGlobalLockHandler;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployVersionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class ChangeFilePermissionApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(ChangeFilePermissionApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getName() {
        return "修改文件权限";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/chmod";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id(当resourceType为workspace时不需要)", type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo(当resourceType为[mirror*|workspace]时不需要)", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID(当resourceType为[build*|workspace]时不需要)", type = ApiParamType.LONG),
            @Param(name = "appSystemId", desc = "应用ID(仅当resourceType为workspace时需要)", type = ApiParamType.LONG),
            @Param(name = "appModuleId", desc = "模块ID(仅当resourceType为workspace时需要)", type = ApiParamType.LONG),
            @Param(name = "resourceType", member = DeployResourceType.class, desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", desc = "目录或文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "mode", desc = "权限(e.g:rwxr-xr-x)", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "修改文件权限")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        String path = paramObj.getString("path");
        String mode = paramObj.getString("mode");
        if (id == null && appSystemId == null && appModuleId == null) {
            throw new ParamNotExistsException("id", "appSystemId", "appModuleId");
        }
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        String runnerUrl;
        String url;
        String fullPath;
        if (!DeployResourceType.WORKSPACE.equals(resourceType)) {
            if (id == null) {
                throw new ParamNotExistsException("id");
            }
            DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
            if (version == null) {
                throw new DeployVersionNotFoundException(id);
            }
            String envName = null;
            if (envId != null) {
                ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
                envName = ciEntityCrossoverService.getCiEntityNameByCiEntityId(envId);
                if (StringUtils.isBlank(envName)) {
                    throw new DeployVersionEnvNotFoundException(version.getVersion(), envId);
                }
            }
            runnerUrl = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            fullPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, path);
        } else {
            runnerUrl = deployVersionService.getWorkspaceRunnerUrl(appSystemId, appModuleId);
            fullPath = deployVersionService.getWorkspaceResourceFullPath(appSystemId, appModuleId, path);
        }
        deployVersionService.checkHomeHasBeenLocked(runnerUrl, fullPath.replace(path, ""));

        url = runnerUrl + "api/rest/file/chmod";
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", fullPath);
        paramJson.put("mode", mode);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new ChangeFilePermissionFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new ChangeFilePermissionFailedException(error);
            }
        }
        return null;
    }


}
