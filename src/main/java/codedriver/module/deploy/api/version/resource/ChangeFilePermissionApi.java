package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.ChangeFilePermissionFailedException;
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
public class ChangeFilePermissionApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(ChangeFilePermissionApi.class);

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
            @Param(name = "path", desc = "目录或文件路径", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "mode", desc = "权限(e.g:rwxr-xr-x)", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "修改文件权限")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        String path = paramObj.getString("path");
        String mode = paramObj.getString("mode");
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", path);
        paramJson.put("mode", mode);
        String url = "autoexecrunner/api/rest/file";
        String method = "/chmod";
        url += method;
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
