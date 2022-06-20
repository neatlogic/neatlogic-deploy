/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.exception.DownloadFileFailedException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class UploadloadFileApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(UploadloadFileApi.class);

    @Override
    public String getToken() {
        return "deploy/version/resource/file/upload";
    }

    @Override
    public String getName() {
        return "上传文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径", isRequired = true),
            @Param(name = "fileParamName", type = ApiParamType.STRING, desc = "文件参数名称", isRequired = true),
            @Param(name = "unpack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否解压")
    })
    @Output({})
    @Description(desc = "上传文件")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // todo 根据应用、模块、版本号、buildNo/环境决定runner与文件路径
        String path = paramObj.getString("path");
        String fileParamName = paramObj.getString("fileParamName");
        Integer unpack = paramObj.getInteger("unpack");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile(fileParamName);
        if (multipartFile != null && multipartFile.getName() != null) {
            String filename = multipartFile.getOriginalFilename();
            InputStream inputStream = multipartFile.getInputStream();
            Map<String, InputStream> fileStreamMap = new HashMap<>();
            fileStreamMap.put(filename, inputStream);
            JSONObject paramJson = new JSONObject();
            paramJson.put("path", path);
            paramJson.put("unpack", unpack);
            String url = "http://bj.ainoe.cn:8080/api/binary/file";
            String method = "/upload";
            url += method;
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setContentType(HttpRequestUtil.ContentType.CONTENT_TYPE_MULTIPART_FORM_DATA_FILE_STREAM).setFormData(paramJson).setFileStreamMap(fileStreamMap).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                throw new DownloadFileFailedException(error);
            }
        }

        return null;
    }
}
