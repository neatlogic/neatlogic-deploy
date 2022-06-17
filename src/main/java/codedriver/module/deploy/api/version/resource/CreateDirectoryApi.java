package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
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
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateDirectoryApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CreateDirectoryApi.class);

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

    // todo 入参待确定
    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "name", desc = "目录名称", isRequired = true, type = ApiParamType.STRING)
    })
    @Description(desc = "新建目录")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        JSONObject paramJson = new JSONObject();
        String path = "/test/hihi"; // todo 根据入参决定path
        paramJson.put("path", path);
        String url = "http://bj.ainoe.cn:8080/api/rest/file/directory/create";
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            logger.error("send request failed.url: {},error: {}", url, error);
            throw new CreateDeployVersionResourceDirectoryFailedException(error);
        }
        JSONObject resultJson = request.getResultJson();
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new CreateDeployVersionResourceDirectoryFailedException(resultJson.getString("Message"));
        }
        return null;
    }
}
