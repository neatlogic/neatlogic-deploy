package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.CreateDirectoryFailedException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
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
public class CreateDirectoryApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CreateDirectoryApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getName() {
        return "新建目录";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/directory/create";
    }

    @Override
    public String getConfig() {
        return null;
    }

    // todo 资源类型名称待定
    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "version_product,env_product,diff_directory,sql_script", desc = "资源类型(version_product:版本制品;env_product:环境制品;diff_directory:差异目录;sql_script:SQL脚本)", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", desc = "目标路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "新建目录")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String resourceType = paramObj.getString("resourceType");
        String path = paramObj.getString("path");
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String url = deployVersionService.getVersionRunnerUrl(paramObj, version);
        url += "api/rest/file/directory/create";
        String fullPath = version.getAppSystemId() + "/"
                + version.getAppModuleId() + "/"
                + version.getVersion() + "/" + (buildNo != null ? "build" + "/" + buildNo : "env" + "/" + envId) + "/"
                + resourceType + "/" + path;
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", fullPath);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new CreateDirectoryFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new CreateDirectoryFailedException(error);
            }
        }
        return null;
    }
}
