package neatlogic.module.deploy.api.version.resource;

import neatlogic.framework.exception.file.FilePathIllegalException;
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
import neatlogic.framework.util.FileSafeUtil;
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
        return "nmdavr.getdirectorycontentapi.getname";
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
            @Param(name = "id", desc = "common.versionid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "nmdavr.getdirectorycontentapi.input.param.desc.buildno", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "nmdavr.getdirectorycontentapi.input.param.desc.envid", type = ApiParamType.LONG),
            @Param(name = "resourceType", member = DeployResourceType.class, desc = "nmdavr.getdirectorycontentapi.input.param.desc.resourcetype", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", desc = "nmdavr.getdirectorycontentapi.input.param.desc.path", isRequired = true, type = ApiParamType.STRING)
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "nmdavr.getdirectorycontentapi.output.param.desc.name"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "nmdavr.getdirectorycontentapi.output.param.desc.type"),
            @Param(name = "size", type = ApiParamType.LONG, desc = "nmdavr.getdirectorycontentapi.output.param.desc.size"),
            @Param(name = "fcd", type = ApiParamType.LONG, desc = "nmdavr.getdirectorycontentapi.output.param.desc.fcd"),
            @Param(name = "fcdText", type = ApiParamType.STRING, desc = "nmdavr.getdirectorycontentapi.output.param.desc.fcdtext"),
            @Param(name = "permission", type = ApiParamType.STRING, desc = "nmdavr.getdirectorycontentapi.output.param.desc.permission"),
            @Param(name = "hasItems", type = ApiParamType.INTEGER, desc = "nmdavr.getdirectorycontentapi.output.param.desc.hasitems")
    })
    @Description(desc = "nmdavr.getdirectorycontentapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String path = getSafeDirectoryPath(paramObj.getString("path"));
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

    private String getSafeDirectoryPath(String path) {
        try {
            String safePath = FileSafeUtil.getSafeRelativePath(path);
            // 目录列表同样先在deploy侧限制为安全相对路径，再交给runner做真实路径边界校验。
            return StringUtils.isBlank(safePath) ? "/" : "/" + safePath;
        } catch (IllegalArgumentException ex) {
            throw new FilePathIllegalException(path);
        }
    }
}
