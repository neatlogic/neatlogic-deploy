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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.importexport.core.ImportExportHandler;
import neatlogic.framework.importexport.core.ImportExportHandlerFactory;
import neatlogic.framework.importexport.dto.*;
import neatlogic.framework.importexport.exception.ImportExportHandlerNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.framework.file.handler.LocalFileSystemHandler;
import neatlogic.module.framework.file.handler.MinioFileSystemHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ImportDeployAppPipelineApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private FileMapper fileMapper;

    private Logger logger = LoggerFactory.getLogger(ImportDeployAppPipelineApi.class);

    @Override
    public String getName() {
        return "导入应用流水线";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "userSelection", type = ApiParamType.JSONOBJECT, desc = "用户的选择数据")
    })
    @Output({
            @Param(name = "typeList", explode = ImportDependencyTypeVo[].class, desc = "依赖项列表")
    })
    @Description(desc = "导入应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        // 获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        // 如果没有导入文件，抛出异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        boolean checkAll = false;
        List<ImportDependencyTypeVo> typeList = new ArrayList<>();
        String userSelection = paramObj.getString("userSelection");
//        System.out.println("userSelection = " + userSelection);
        if (StringUtils.isNotBlank(userSelection)) {
            JSONObject userSelectionObj = JSONObject.parseObject(userSelection);
            if (MapUtils.isNotEmpty(userSelectionObj)) {
                checkAll = userSelectionObj.getBooleanValue("checkAll");
                JSONArray typeArray = userSelectionObj.getJSONArray("typeList");
                if (CollectionUtils.isNotEmpty(typeArray)) {
                    typeList = typeArray.toJavaList(ImportDependencyTypeVo.class);
                }
            }
        }
        byte[] buf = new byte[1024];
        // 遍历导入文件
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //
            try (ZipInputStream zipIs = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()
            ) {
                int index = 0;
                ImportExportVo mainImportExportVo = null;
                List<ImportExportPrimaryChangeVo> primaryChangeList = new ArrayList<>();
                Map<Long, FileVo> fileMap = new HashMap<>();
                ZipEntry zipEntry = null;
                while ((zipEntry = zipIs.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    out.reset();
                    System.out.println("zipEntry.getName() = " + zipEntry.getName());
                    if (index == 0) {
                        int len;
                        while ((len = zipIs.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        mainImportExportVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                        if (!Objects.equals(mainImportExportVo.getName(), appSystem.getAbbrName())) {

                        }
                        ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(mainImportExportVo.getType());
                        if (importExportHandler == null) {
                            throw new ImportExportHandlerNotFoundException(mainImportExportVo.getType());
                        }
                        List<ImportExportBaseInfoVo> dependencyBaseInfoList = mainImportExportVo.getDependencyBaseInfoList();
                        if (CollectionUtils.isNotEmpty(dependencyBaseInfoList) && StringUtils.isBlank(userSelection)) {
                            List<ImportDependencyTypeVo> importDependencyTypeList = importExportHandler.checkDependencyList(dependencyBaseInfoList);
                            if (CollectionUtils.isNotEmpty(importDependencyTypeList)) {
                                JSONObject resultObj = new JSONObject();
                                resultObj.put("checkedAll", false);
                                resultObj.put("typeList", importDependencyTypeList);
                                System.out.println("resultObj = " + resultObj);
                                return resultObj;
                            }
                            checkAll = true;
                        }
                    } else {
                        if (zipEntry.getName().startsWith("dependency-folder/")) {
                            int len;
                            while ((len = zipIs.read(buf)) != -1) {
                                out.write(buf, 0, len);
                            }
                            ImportExportVo dependencyVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), ImportExportVo.class);
                            String type = dependencyVo.getType();
                            boolean flag = true;
                            if (!checkAll) {
                                for (ImportDependencyTypeVo typeVo : typeList) {
                                    if (Objects.equals(typeVo.getValue(), type)) {
                                        if (!Objects.equals(typeVo.getCheckedAll(), true)) {
                                            for (ImportDependencyOptionVo optionVo : typeVo.getOptionList()) {
                                                if (Objects.equals(optionVo.getValue(), dependencyVo.getPrimaryKey())) {
                                                    if (!Objects.equals(optionVo.getChecked(), true)) {
                                                        flag = false;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(type);
                                if (importExportHandler == null) {
                                    throw new ImportExportHandlerNotFoundException(type);
                                }
                                Object oldPrimaryKey = dependencyVo.getPrimaryKey();
//                                Long newPrimaryKey = importExportHandler.importData(dependencyVo, primaryChangeList);
//                                if (!Objects.equals(oldPrimaryKey, newPrimaryKey)) {
//                                    primaryChangeList.add(new ImportExportPrimaryChangeVo(type, oldPrimaryKey, newPrimaryKey));
//                                }
                                if (Objects.equals(type, "file")) {
                                    FileVo fileVo = dependencyVo.getData().toJavaObject(FileVo.class);
                                    fileMap.put(fileVo.getId(), fileVo);
                                }
                            }
                        } else if (zipEntry.getName().startsWith("attachment-folder/")) {
                            String tenantUuid = TenantContext.get().getTenantUuid();
                            String fileName = zipEntry.getName();
                            int beginIndex = fileName.indexOf("/");
                            int endIndex = fileName.indexOf("/", beginIndex + 1);
                            String fileIdStr = fileName.substring(beginIndex + 1, endIndex);
                            Long fileId = Long.valueOf(fileIdStr);
                            FileVo fileVo = fileMap.get(fileId);
                            Object newPrimary = getNewPrimaryKey("file", fileId, primaryChangeList);
                            if (newPrimary != null) {
                                fileVo.setId((Long) newPrimary);
                            }
                            int len;
                            while ((len = zipIs.read(buf)) != -1) {
                                out.write(buf, 0, len);
                            }
                            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                            String filePath;
                            try {
                                filePath = FileUtil.saveData(MinioFileSystemHandler.NAME, tenantUuid, in, fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                            } catch (Exception ex) {
                                //如果没有配置minioUrl，则表示不使用minio，无需抛异常
                                if (StringUtils.isNotBlank(Config.MINIO_URL())) {
                                    logger.error(ex.getMessage(), ex);
                                }
                                // 如果minio出现异常，则上传到本地
                                filePath = FileUtil.saveData(LocalFileSystemHandler.NAME, tenantUuid, in, fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                            } finally {
                                in.close();
                            }
                            fileVo.setPath(filePath);
                            fileMapper.updateFile(fileVo);
                        }
                    }
                    index++;
                }
//                ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(mainImportExportVo.getType());
//                importExportHandler.importData(mainImportExportVo, primaryChangeList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/import";
    }


    private Object getNewPrimaryKey(String type, Object oldPrimary, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        for (ImportExportPrimaryChangeVo primaryChangeVo : primaryChangeList) {
            if (Objects.equals(primaryChangeVo.getType(), type) && Objects.equals(primaryChangeVo.getOldPrimaryKey(), oldPrimary)) {
                return primaryChangeVo.getNewPrimaryKey();
            }
        }
        return null;
    }
}
