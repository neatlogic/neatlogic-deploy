/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.deploy.exception.DeployPinelineTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.module.deploy.dao.mapper.DeployPipelineTemplateMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 导出组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployPipelineTemplateExportApi extends PrivateBinaryStreamApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(DeployPipelineTemplateExportApi.class);

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/export";
    }

    @Override
    public String getName() {
        return "导出流水线模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "流水线模板id列表")
    })
    @Description(desc = "导出流水线模板")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Long> idList = paramObj.getJSONArray("idList").toJavaList(Long.class);
        if (CollectionUtils.isEmpty(idList)) {
            throw new ParamNotExistsException("idList");
        }
        List<Long> existIdList = deployPipelineTemplateMapper.checkPinelineTemplateIdListIsExists(idList);
        idList.removeAll(existIdList);
        if (CollectionUtils.isNotEmpty(idList)) {
            int capacity = 17 * idList.size();
            System.out.println(capacity);
            StringBuilder stringBuilder = new StringBuilder(capacity);
            for (Long id : idList) {
                stringBuilder.append(id);
                stringBuilder.append("、");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            System.out.println(stringBuilder.length());
            throw new DeployPinelineTemplateNotFoundException(stringBuilder.toString());
        }
        List<DeployPipelineTemplateVo> deployPipelineTemplateVoList = new ArrayList<>();
        for (Long id : existIdList) {
            DeployPipelineTemplateVo deployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
            deployPipelineTemplateVoList.add(deployPipelineTemplateVo);
        }
        //设置导出文件名
        String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), "流水线模板." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("aplication/zip");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        try (ZipOutputStream zipos = new ZipOutputStream(response.getOutputStream())) {
            for (DeployPipelineTemplateVo deployPipelineTemplateVo : deployPipelineTemplateVoList) {
                zipos.putNextEntry(new ZipEntry(deployPipelineTemplateVo.getName() + ".json"));
                zipos.write(JSONObject.toJSONBytes(deployPipelineTemplateVo));
                zipos.closeEntry();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
