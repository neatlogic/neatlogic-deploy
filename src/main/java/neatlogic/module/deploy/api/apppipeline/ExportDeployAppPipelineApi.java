/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandler;
import neatlogic.framework.importexport.core.ImportExportHandlerFactory;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.framework.importexport.exception.ExportNoAuthException;
import neatlogic.framework.importexport.exception.ImportExportHandlerNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ExportDeployAppPipelineApi extends PrivateBinaryStreamApiComponentBase {

    private Logger logger = LoggerFactory.getLogger(ExportDeployAppPipelineApi.class);

    @Override
    public String getName() {
        return "nmdaa.exportdeployapppipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid")
    })
    @Output({})
    @Description(desc = "nmdaa.exportdeployapppipelineapi.getname")
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
        if (!importExportHandler.checkExportAuth(appSystemId)) {
            throw new ExportNoAuthException();
        }
        // 先检查导出对象及依赖对象有没有找不到数据，如果有就抛异常
        {
            List<ImportExportBaseInfoVo> dependencyBaseInfoList = new ArrayList<>();
            dependencyBaseInfoList.add(new ImportExportBaseInfoVo(DeployImportExportHandlerType.APP_PIPELINE.getValue(), appSystemId));
            ImportExportVo importExportVo = importExportHandler.exportData(appSystemId, dependencyBaseInfoList, null);
        }
        String fileName = FileUtil.getEncodedFileName("appSystem_" + appSystem.getAbbrName()+ "(" + appSystem.getName() + ")" + ".pak");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        List<ImportExportBaseInfoVo> dependencyBaseInfoList = new ArrayList<>();
        try (ZipOutputStream zipos = new ZipOutputStream(response.getOutputStream())) {
            ImportExportVo importExportVo = importExportHandler.exportData(appSystemId, dependencyBaseInfoList, zipos);
            importExportVo.setDependencyBaseInfoList(dependencyBaseInfoList);
            zipos.putNextEntry(new ZipEntry(importExportVo.getPrimaryKey() + ".json"));
            zipos.write(JSONObject.toJSONBytes(importExportVo));
            zipos.closeEntry();
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
