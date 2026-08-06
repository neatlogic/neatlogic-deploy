/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.deploy.api.version.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.ResponseCode;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionResourceTypeNotFoundException;
import neatlogic.framework.exception.file.UploadFileFailedException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployVersionService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class UploadloadFileApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(UploadloadFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/version/resource/file/upload";
    }

    @Override
    public String getName() {
        return "nmdavr.uploadloadfileapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "common.versionid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "nmdavr.uploadloadfileapi.input.param.desc.buildno", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "nmdavr.uploadloadfileapi.input.param.desc.envid", type = ApiParamType.LONG),
            @Param(name = "resourceType", member = DeployResourceType.class, desc = "nmdavr.uploadloadfileapi.input.param.desc.resourcetype", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "path", type = ApiParamType.STRING, desc = "nmdavr.uploadloadfileapi.input.param.desc.path", isRequired = true),
            @Param(name = "unpack", type = ApiParamType.ENUM, rule = "1,0", desc = "nmdavr.uploadloadfileapi.input.param.desc.unpack"),
            @Param(name = "fileParamName", type = ApiParamType.STRING, desc = "nmdavr.uploadloadfileapi.input.param.desc.fileparamname", isRequired = true),
    })
    @Output({})
    @Description(desc = "nmdavr.uploadloadfileapi.getname")
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

        //校验环境权限、校验版本&制品管理的操作权限
        if (envId != null) {
            deployAppAuthorityService.checkEnvAuth(version.getAppSystemId(), envId);
        }
        deployAppAuthorityService.checkOperationAuth(version.getAppSystemId(), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);

        String runnerUrl;
        String url;
        String fullPath;
        if (!DeployResourceType.WORKSPACE.equals(resourceType)) {
            String envName = deployVersionService.getEnvName(version.getVersion(), envId);
            runnerUrl = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
            fullPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, path);
        } else {
            runnerUrl = deployVersionService.getWorkspaceRunnerUrl(version);
            fullPath = deployVersionService.getWorkspaceResourceFullPath(version.getAppSystemId(), version.getAppModuleId(), path);
        }
        deployVersionService.checkHomeHasBeenLocked(runnerUrl, fullPath.replace(path, ""));

        url = runnerUrl + "api/binary/file/upload";
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile(fileParamName);
        if (multipartFile != null && multipartFile.getName() != null) {
            String filename = multipartFile.getOriginalFilename();
            InputStream inputStream = multipartFile.getInputStream();
            Map<String, InputStream> fileStreamMap = new HashMap<>();
            fileStreamMap.put(filename, inputStream);
            JSONObject paramJson = new JSONObject();
            paramJson.put("path", fullPath + "/" + filename);
            paramJson.put("unpack", unpack);
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setContentType(HttpRequestUtil.ContentType.CONTENT_TYPE_MULTIPART_FORM_DATA_FILE_STREAM).setFormData(paramJson).setFileStreamMap(fileStreamMap).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            int responseCode = httpRequestUtil.getResponseCode();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                if (responseCode == ResponseCode.API_RUNTIME.getCode()) {
                    throw new UploadFileFailedException(JSONObject.parseObject(error).getString("Message"));
                } else {
                    throw new UploadFileFailedException(error);
                }
            }
            deployVersionService.syncProjectFile(version, runnerUrl, Collections.singletonList(fullPath + "/" + filename));

        }

        return null;
    }
}
