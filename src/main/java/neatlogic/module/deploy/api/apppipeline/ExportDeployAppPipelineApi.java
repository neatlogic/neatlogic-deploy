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
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.importexport.core.ImportExportHandler;
import neatlogic.framework.importexport.core.ImportExportHandlerFactory;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.framework.importexport.exception.ImportExportHandlerNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.deploy.constvalue.DeployImportExportHandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ExportDeployAppPipelineApi extends PrivateBinaryStreamApiComponentBase {

    private Logger logger = LoggerFactory.getLogger(ExportDeployAppPipelineApi.class);

    @Override
    public String getName() {
        return "导出应用流水线";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID")
    })
    @Output({})
    @Description(desc = "导出应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundEditTargetException(appSystemId);
        }
        ImportExportHandler importExportHandler = ImportExportHandlerFactory.getHandler(DeployImportExportHandlerType.APP_PIPELINE.getValue());
        if (importExportHandler == null) {
            throw new ImportExportHandlerNotFoundException(DeployImportExportHandlerType.APP_PIPELINE.getText());
        }
        List<ImportExportVo> dependencyList = new ArrayList<>();
        ImportExportVo importExportVo = importExportHandler.exportData(appSystemId, dependencyList);
        List<ImportExportVo> fileImportExportList = new ArrayList<>();
        List<ImportExportBaseInfoVo> dependencyBaseInfoList = new ArrayList<>();
        for (ImportExportVo dependency : dependencyList) {
            dependencyBaseInfoList.add(new ImportExportBaseInfoVo(dependency.getType(), dependency.getPrimaryKey(), dependency.getName()));
            if (Objects.equals(dependency.getType(), "file")) {
                fileImportExportList.add(dependency);
            }
        }
        importExportVo.setDependencyBaseInfoList(dependencyBaseInfoList);
        String fileName = FileUtil.getEncodedFileName("应用系统_" + appSystem.getAbbrName()+ "(" + appSystem.getName() + ")" + ".pak");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        byte[] buf = new byte[1024];
        try (ZipOutputStream zipos = new ZipOutputStream(response.getOutputStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            zipos.putNextEntry(new ZipEntry(importExportVo.getPrimaryKey() + ".json"));
            zipos.write(JSONObject.toJSONBytes(importExportVo));
            zipos.closeEntry();
            for (ImportExportVo importExport : dependencyList) {
                zipos.putNextEntry(new ZipEntry("dependency-folder/" + importExport.getPrimaryKey() + ".json"));
                zipos.write(JSONObject.toJSONBytes(importExport));
                zipos.closeEntry();
            }
            for (ImportExportVo importExport : fileImportExportList) {
                FileVo fileVo = importExport.getData().toJavaObject(FileVo.class);
                InputStream in = neatlogic.framework.common.util.FileUtil.getData(fileVo.getPath());
                if (in != null) {
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    zipos.putNextEntry(new ZipEntry("attachment-folder/" + fileVo.getId() + "/" + fileVo.getName()));
                    zipos.write(out.toByteArray());
                    zipos.closeEntry();
                    in.close();
                    out.reset();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/export";
    }
}
