package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.ChangeDeployVersionResourcePermissionFailedException;
import codedriver.framework.deploy.exception.CreateDeployVersionResourceDirectoryFailedException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class ChangePermissionApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(ChangePermissionApi.class);

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

    // todo 入参待确定
    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "name", desc = "目录或文件名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "mode", desc = "权限(e.g:rwxr-xr-x)", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "修改文件权限")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        String name = paramObj.getString("name");
        String mode = paramObj.getString("mode");
        JSONObject paramJson = new JSONObject();
        String path = "/test/hihi"; // todo 根据入参决定path
        paramJson.put("path", path);
        paramJson.put("mode", mode);
        String url = "http://bj.ainoe.cn:8080/api/rest/file";
        String method = "/chmod";
        url += method;
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            logger.error("send request failed.url: {},error: {}", url, error);
            throw new ChangeDeployVersionResourcePermissionFailedException(error);
        }
        JSONObject resultJson = request.getResultJson();
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new ChangeDeployVersionResourcePermissionFailedException(resultJson.getString("Message"));
        }
        return null;
    }
}
