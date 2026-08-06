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

package neatlogic.module.deploy.api.pipeline;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.framework.deploy.exception.DeployPipelineNotFoundException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeletePipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper pipelineMapper;
    @Resource
    private DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdap.deletepipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/delete";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "nmdap.deletepipelineapi.input.param.desc.id", isRequired = true)})
    @Description(desc = "nmdap.deletepipelineapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        PipelineVo pipelineVo = pipelineMapper.getPipelineById(id);
        if (pipelineVo == null) {
            throw new DeployPipelineNotFoundException(id);
        }
        String type = pipelineVo.getType();
        if (Objects.equals(type, PipelineType.GLOBAL.getValue())) {
            if (!AuthActionChecker.check(PIPELINE_MODIFY.class)) {
                throw new PermissionDeniedException(PIPELINE_MODIFY.class);
            }
        } else if (Objects.equals(type, PipelineType.APPSYSTEM.getValue())) {
            deployAppAuthorityService.checkOperationAuth(pipelineVo.getAppSystemId(), DeployAppConfigAction.PIPELINE);
        }
        pipelineMapper.deleteLaneGroupJobTemplateByPipelineId(id);
        pipelineMapper.deleteLaneGroupJobTemplateByPipelineId(id);
        pipelineMapper.deletePipelineById(id);
        return null;
    }

}
