package neatlogic.module.deploy.api.version.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.CopyOrMoveFileToSubDirectoryException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import neatlogic.framework.deploy.exception.MoveFileFailedException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployVersionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class MoveFileApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(MoveFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "文件移动或重命名";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/move";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo(当resourceType为[mirror*|workspace]时不需要)", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID(当resourceType为[build*|workspace]时不需要)", type = ApiParamType.LONG),
            @Param(name = "resourceType", member = DeployResourceType.class, desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "name", desc = "新文件名(重命名时需指定)", rule = RegexUtils.NAME, maxLength = 50, type = ApiParamType.REGEX),
            @Param(name = "src", desc = "源文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "dest", desc = "目标文件路径(路径一律以'/'开头，HOME本身的路径为'/')", type = ApiParamType.STRING),
            @Param(name = "operation", desc = "操作(move:移动;rename:重命名)", rule = "move,rename", isRequired = true, type = ApiParamType.ENUM)
    })
    @Description(desc = "文件移动或重命名")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String name = paramObj.getString("name");
        String src = paramObj.getString("src");
        String dest = paramObj.getString("dest");
        String operation = paramObj.getString("operation");
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        if ("move".equals(operation)) {
            if (StringUtils.isBlank(dest)) {
                throw new ParamNotExistsException("dest");
            }
            if (Objects.equals(src, dest)) {
                return null;
            }
            if (dest.startsWith(src)) {
                throw new CopyOrMoveFileToSubDirectoryException();
            }
        }
        if ("rename".equals(operation) && StringUtils.isBlank(name)) {
            throw new ParamNotExistsException("name");
        }
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }

        //校验环境权限、校验版本&制品管理的操作权限
        if (envId != null) {
            deployAppAuthorityService.checkEnvAuth(version.getAppSystemId(), envId);
        }
        deployAppAuthorityService.checkOperationAuth(version.getAppSystemId(), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);

        String runnerUrl;
        String url;
        String fullSrcPath;
        String fullDestPath;
        if (!DeployResourceType.WORKSPACE.equals(resourceType)) {
            String envName = deployVersionService.getEnvName(version.getVersion(), envId);
            runnerUrl = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            fullSrcPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, src);
            if ("move".equals(operation)) {
                fullDestPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, dest + src.substring(src.lastIndexOf("/")));
            } else {
                fullDestPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, src.substring(0, src.lastIndexOf("/")) + "/" + name);
            }
        } else {
            runnerUrl = deployVersionService.getWorkspaceRunnerUrl(version);
            fullSrcPath = deployVersionService.getWorkspaceResourceFullPath(version.getAppSystemId(), version.getAppModuleId(), src);
            if ("move".equals(operation)) {
                fullDestPath = deployVersionService.getWorkspaceResourceFullPath(version.getAppSystemId(), version.getAppModuleId(), dest + src.substring(src.lastIndexOf("/")));
            } else {
                fullDestPath = deployVersionService.getWorkspaceResourceFullPath(version.getAppSystemId(), version.getAppModuleId(), src.substring(0, src.lastIndexOf("/")) + "/" + name);
            }
        }
        deployVersionService.checkHomeHasBeenLocked(runnerUrl, fullSrcPath.replace(src, ""));

        url = runnerUrl + "api/rest/file/move";
        JSONObject paramJson = new JSONObject();
        paramJson.put("src", fullSrcPath);
        paramJson.put("dest", fullDestPath);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
                throw new MoveFileFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new MoveFileFailedException(error);
            }
        }
        deployVersionService.syncProjectFile(version, runnerUrl, Arrays.asList(fullSrcPath, fullDestPath));
        return null;
    }
}
