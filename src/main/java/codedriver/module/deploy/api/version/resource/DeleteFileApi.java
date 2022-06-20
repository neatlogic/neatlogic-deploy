package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.DeleteFileFailedException;
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
public class DeleteFileApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(DeleteFileApi.class);

    @Override
    public String getName() {
        return "删除文件";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    // todo 入参待确定
    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "path", desc = "目录或文件路径", isRequired = true, type = ApiParamType.STRING),
    })
    @Description(desc = "删除文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        String path = paramObj.getString("path");
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", path);
        String url = "autoexecrunner/api/rest/file";
        String method = "/delete";
        url += method;
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (responseCode == 520) {
            JSONObject resultJson = request.getResultJson();
            throw new DeleteFileFailedException(resultJson.getString("Message"));
        } else if (StringUtils.isNotBlank(error)) {
            throw new DeleteFileFailedException(error);
        }
        return null;
    }
}
