package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.GetDirectoryFailedException;
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
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "version_product,env_product,diff_directory,sql_script", desc = "资源类型(version_product:版本制品;env_product:环境制品;diff_directory:差异目录;sql_script:SQL脚本)", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", desc = "目标路径", isRequired = true, type = ApiParamType.STRING)
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
        String resourceType = paramObj.getString("resourceType");
        String path = paramObj.getString("path");
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String url = deployVersionService.getVersionRunnerUrl(paramObj, version);
        url += "api/rest/file/directory/content/get";
        String fullPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envId, path);
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", fullPath);
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
