package codedriver.module.deploy.api.version.resource;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import codedriver.framework.deploy.exception.GetDirectoryFailedException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
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

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
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
            @Param(name = "id", desc = "版本id", type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "appSystemId", desc = "应用ID(仅当resourceType为工程目录时需要)", type = ApiParamType.LONG),
            @Param(name = "appModuleId", desc = "模块ID(仅当resourceType为工程目录时需要)", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "build_product,build_sql_script,env_product,env_diff_directory,env_sql_script,mirror_product,mirror_diff,workspace", desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", desc = "目标路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING)
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "文件名"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "文件类型"),
            @Param(name = "size", type = ApiParamType.LONG, desc = "文件大小"),
            @Param(name = "fcd", type = ApiParamType.LONG, desc = "最后修改时间"),
            @Param(name = "fcdText", type = ApiParamType.STRING, desc = "最后修改时间(格式化为yyyy-MM-dd HH:mm:ss)"),
            @Param(name = "permission", type = ApiParamType.STRING, desc = "文件权限")
    })
    @Description(desc = "获取目录内容")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        String path = paramObj.getString("path");
        if (id == null && appSystemId == null && appModuleId == null) {
            throw new ParamNotExistsException("id", "appSystemId", "appModuleId");
        }
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        String url = null;
        String homePath = null;
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
            url = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            homePath = deployVersionService.getVersionResourceHomePath(version, resourceType, buildNo, envName);
        } else {
            url = deployVersionService.getWorkspaceRunnerUrl(appSystemId, appModuleId);
            homePath = resourceType.getDirectoryName() + "/";
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
            if (responseCode == 520) {
                throw new GetDirectoryFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new GetDirectoryFailedException(error);
            }
        }
        return resultJson.getJSONArray("Return");
    }
}
