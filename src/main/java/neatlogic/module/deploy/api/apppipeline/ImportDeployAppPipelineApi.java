/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployImportExportHandlerType;
import neatlogic.framework.deploy.exception.pipeline.ImportDeployPipelineAppNameInconsistencyException;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.importexport.core.ImportExportHandlerFactory;
import neatlogic.framework.importexport.dto.ImportDependencyTypeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ImportDeployAppPipelineApi extends PrivateBinaryStreamApiComponentBase {

    private Logger logger = LoggerFactory.getLogger(ImportDeployAppPipelineApi.class);

    @Override
    public String getName() {
        return "nmdaa.importdeployapppipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "userSelection", type = ApiParamType.JSONOBJECT, desc = "common.userselectionimportoption")
    })
    @Output({
            @Param(name = "typeList", explode = ImportDependencyTypeVo[].class, desc = "commom.tbodylist")
    })
    @Description(desc = "nmdaa.importdeployapppipelineapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        // 获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        // 如果没有导入文件，抛出异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }


        MultipartFile multipartFile = null;
        // 遍历导入文件
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            multipartFile = entry.getValue();
            break;
        }
        if (multipartFile != null) {
            String userSelection = paramObj.getString("userSelection");
            // 遍历压缩包，检查导入数据应用系统名称与目标应用系统名称是否相同，不相同抛异常
            if (StringUtils.isBlank(userSelection)) {
                try (ZipInputStream zipIs = new ZipInputStream(multipartFile.getInputStream());
                     ByteArrayOutputStream out = new ByteArrayOutputStream()
                ) {
                    byte[] buf = new byte[1024];
                    ZipEntry zipEntry = null;
                    while ((zipEntry = zipIs.getNextEntry()) != null) {
                        if (zipEntry.isDirectory()) {
                            continue;
                        }
                        out.reset();
                        String zipEntryName = zipEntry.getName();
                        if (zipEntryName.startsWith("dependency-folder/")) {
                            continue;
                        }
                        if (zipEntryName.startsWith("attachment-folder/")) {
                            continue;
                        }
                        if (zipEntry.getName().endsWith(".json")) {
                            int len;
                            while ((len = zipIs.read(buf)) != -1) {
                                out.write(buf, 0, len);
                            }
                            ImportExportVo mainImportExportVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                            if (!Objects.equals(mainImportExportVo.getName(), appSystem.getAbbrName())) {
                                throw new ImportDeployPipelineAppNameInconsistencyException(mainImportExportVo.getName(), appSystem.getAbbrName());
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return ImportExportHandlerFactory.importData(multipartFile, DeployImportExportHandlerType.APP_PIPELINE.getValue(), userSelection);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/import";
    }
}