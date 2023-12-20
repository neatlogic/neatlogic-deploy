package neatlogic.module.deploy.api.version.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import neatlogic.framework.deploy.exception.GetDirectoryFailedException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployVersionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDirectoryContentApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(GetDirectoryContentApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getName() {
        return "获取目录内容";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/directory/content/get";
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
            @Param(name = "path", desc = "目标路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING)
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "文件名"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "文件类型"),
            @Param(name = "size", type = ApiParamType.LONG, desc = "文件大小"),
            @Param(name = "fcd", type = ApiParamType.LONG, desc = "最后修改时间"),
            @Param(name = "fcdText", type = ApiParamType.STRING, desc = "最后修改时间(格式化为yyyy-MM-dd HH:mm:ss)"),
            @Param(name = "permission", type = ApiParamType.STRING, desc = "文件权限"),
            @Param(name = "hasItems", type = ApiParamType.INTEGER, desc = "目录是否有内容")
    })
    @Description(desc = "获取目录内容")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String path = paramObj.getString("path");
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String url;
        String homePath;
        if (!DeployResourceType.WORKSPACE.equals(resourceType)) {
            String envName = deployVersionService.getEnvName(version.getVersion(), envId);
            url = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            homePath = deployVersionService.getVersionResourceHomePath(version, resourceType, buildNo, envName);
        } else {
            url = deployVersionService.getWorkspaceRunnerUrl(version);
            homePath = deployVersionService.getWorkspaceResourceHomePath(version.getAppSystemId(), version.getAppModuleId());
        }
        url += "api/rest/file/directory/content/get";
        JSONObject paramJson = new JSONObject();
        paramJson.put("home", homePath);
        paramJson.put("path", path);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        JSONObject resultJson = request.getResultJson();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
                throw new GetDirectoryFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new GetDirectoryFailedException(error);
            }
        }
        return resultJson.getJSONArray("Return");
    }
}
