package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.MoveFileFailedException;
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
public class MoveFileApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(MoveFileApi.class);

    @Override
    public String getName() {
        return "文件复制或重命名";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/move";
    }

    @Override
    public String getConfig() {
        return null;
    }

    // todo 入参待确定
    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "src", desc = "源文件路径", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "dest", desc = "目标文件路径", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "文件复制或重命名")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String src = paramObj.getString("src");
        String dest = paramObj.getString("dest");
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        JSONObject paramJson = new JSONObject();
        paramJson.put("src", src);
        paramJson.put("dest", dest); // todo 截取src最后的文件名拼到dest
        String url = "autoexecrunner/api/rest/file";
        String method = "/move";
        url += method;
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new MoveFileFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new MoveFileFailedException(error);
            }
        }
        return null;
    }
}
