/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.PIPELINE_MODIFY;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.deploy.exception.DeployPipelineNotFoundException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.PipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = PIPELINE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeletePipelineApi extends PrivateApiComponentBase {
    @Resource
    private PipelineMapper pipelineMapper;


    @Override
    public String getName() {
        return "删除超级流水线";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/delete";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Description(desc = "删除超级流水线接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        PipelineVo pipelineVo = pipelineMapper.getPipelineById(id);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundException(id);
        }
        String type = pipelineVo.getType();
        if (Objects.equals(type, PipelineType.GLOBAL.getValue())) {
            if (AuthActionChecker.check(PIPELINE_MODIFY.class)) {
                throw new PermissionDeniedException(PIPELINE_MODIFY.class);
            }
        } else if (Objects.equals(type, PipelineType.APPSYSTEM.getValue())) {
            if (AuthActionChecker.check(DEPLOY_BASE.class)) {
                throw new PermissionDeniedException(DEPLOY_BASE.class);
            }
        }
        pipelineMapper.deleteLaneGroupJobTemplateByPipelineId(id);
        pipelineMapper.deleteLaneGroupJobTemplateByPipelineId(id);
        pipelineMapper.deletePipelineById(id);
        return null;
    }

}
