/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.DownloadFileFailedException;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadFileApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(DownloadFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getToken() {
        return "deploy/version/resource/file/download";
    }

    @Override
    public String getName() {
        return "下载文件";
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
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径", isRequired = true),
            @Param(name = "isPack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否打包")
    })
    @Output({})
    @Description(desc = "下载文件(若选择打包下载，下载的文件为压缩包)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String resourceType = paramObj.getString("resourceType");
        String path = paramObj.getString("path");
        Integer isPack = paramObj.getInteger("isPack");
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String url = deployVersionService.getVersionRunnerUrl(paramObj, version);
        url += "api/binary/file/download";
        // todo 路径待定
        String fullPath = version.getAppSystemId() + "/"
                + version.getAppModuleId() + "/"
                + version.getVersion() + "/" + (buildNo != null ? "build" + "/" + buildNo : "env" + "/" + envId) + "/"
                + resourceType + "/" + path;
        JSONObject paramJson = new JSONObject();
        paramJson.put("path", fullPath);
        paramJson.put("isPack", isPack);
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream()).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = httpRequestUtil.getResponseCode();
        String error = httpRequestUtil.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new DownloadFileFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new DownloadFileFailedException(error);
            }
        }
        return null;
    }
}
