package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.CopyFileFailedException;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
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

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class CopyFileApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CopyFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getName() {
        return "复制文件";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/copy";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "build_product,build_sql_script,env_product,env_diff_directory,env_sql_script,mirror_product,mirror_diff,mirror_sql_script", desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "src", desc = "源文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "dest", desc = "目标目录路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "复制文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String src = paramObj.getString("src");
        String dest = paramObj.getString("dest");
        DeployResourceType resourceType = DeployResourceType.getDeployResourceType(paramObj.getString("resourceType"));
        if (resourceType == null) {
            throw new DeployVersionResourceTypeNotFoundException(paramObj.getString("resourceType"));
        }
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String envName = null;
        if (envId != null) {
            envName = deployVersionService.getVersionEnvNameByEnvId(envId);
            if (StringUtils.isBlank(envName)) {
                throw new DeployVersionEnvNotFoundException(version.getVersion(), envId);
            }
        }
        String url = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
        url += "api/rest/file/copy";
        String fullSrcPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, src);
        String fullDestPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, dest + src.substring(src.lastIndexOf("/")));
        JSONObject paramJson = new JSONObject();
        paramJson.put("src", fullSrcPath);
        paramJson.put("dest", fullDestPath);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new CopyFileFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new CopyFileFailedException(error);
            }
        }
        return null;
    }
}
