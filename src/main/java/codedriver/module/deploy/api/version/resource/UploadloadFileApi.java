/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import codedriver.framework.deploy.exception.UploadFileFailedException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployVersionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class UploadloadFileApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(UploadloadFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

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
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "build_product,build_sql_script,env_product,env_diff_directory,env_sql_script,mirror_product,mirror_diff,mirror_sql_script", desc = "制品类型", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true),
            @Param(name = "unpack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否解压"),
            @Param(name = "fileParamName", type = ApiParamType.STRING, desc = "文件参数名称", isRequired = true),
    })
    @Output({})
    @Description(desc = "上传文件")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String path = paramObj.getString("path");
        String fileParamName = paramObj.getString("fileParamName");
        Integer unpack = paramObj.getInteger("unpack");
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
        url += "api/binary/file/upload";
        String fullPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, path);
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile(fileParamName);
        if (multipartFile != null && multipartFile.getName() != null) {
            String filename = multipartFile.getOriginalFilename();
            InputStream inputStream = multipartFile.getInputStream();
            Map<String, InputStream> fileStreamMap = new HashMap<>();
            fileStreamMap.put(filename, inputStream);
            JSONObject paramJson = new JSONObject();
            paramJson.put("path", fullPath);
            paramJson.put("unpack", unpack);
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setContentType(HttpRequestUtil.ContentType.CONTENT_TYPE_MULTIPART_FORM_DATA_FILE_STREAM).setFormData(paramJson).setFileStreamMap(fileStreamMap).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == 520) {
                    throw new UploadFileFailedException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new UploadFileFailedException(error);
                }
            }
        }

        return null;
    }
}
