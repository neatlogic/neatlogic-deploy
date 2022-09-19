/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.PIPELINE_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.dto.pipeline.*;
import codedriver.framework.deploy.exception.DeployPipelineNotFoundException;
import codedriver.framework.deploy.exception.DeployScheduleNameRepeatException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SavePipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper pipelineMapper;
    @Resource
    private DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "保存超级流水线";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/save";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表添加新流水线"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "作业名称"),
            @Param(name = "type", type = ApiParamType.ENUM, member = PipelineType.class, isRequired = true, desc = "类型"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "laneList", type = ApiParamType.JSONARRAY, desc = "通道列表"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表")})
    @ResubmitInterval(3)
    @Output({@Param(explode = PipelineVo.class)})
    @Description(desc = "保存超级流水线接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (id != null) {
            PipelineVo pipelineVo = pipelineMapper.getPipelineById(id);
            if (pipelineVo == null) {
                throw new DeployPipelineNotFoundException(id);
            }
        }
        PipelineVo pipelineVo = JSONObject.toJavaObject(jsonObj, PipelineVo.class);
        String type = pipelineVo.getType();
        if (Objects.equals(type, PipelineType.GLOBAL.getValue())) {
            if (!AuthActionChecker.check(PIPELINE_MODIFY.class)) {
                throw new PermissionDeniedException(PIPELINE_MODIFY.class);
            }
        } else if (Objects.equals(type, PipelineType.APPSYSTEM.getValue())) {
            deployAppAuthorityService.checkOperationAuth(pipelineVo.getAppSystemId(), DeployAppConfigAction.PIPELINE);
        }
        if (id == null) {
            pipelineVo.setFcu(UserContext.get().getUserUuid(true));
            pipelineMapper.insertPipeline(pipelineVo);
        } else {
            pipelineVo.setLcu(UserContext.get().getUserUuid(true));
            pipelineMapper.updatePipeline(pipelineVo);
            pipelineMapper.deleteLaneGroupJobTemplateByPipelineId(pipelineVo.getId());
            pipelineMapper.deletePipelineAuthByPipelineId(pipelineVo.getId());
        }
        if (CollectionUtils.isNotEmpty(pipelineVo.getLaneList())) {
            for (int i = 0; i < pipelineVo.getLaneList().size(); i++) {
                PipelineLaneVo laneVo = pipelineVo.getLaneList().get(i);
                boolean hasLaneJob = false;
                if (CollectionUtils.isNotEmpty(laneVo.getGroupList())) {
                    for (int j = 0; j < laneVo.getGroupList().size(); j++) {
                        PipelineGroupVo groupVo = laneVo.getGroupList().get(j);
                        boolean hasGroupJob = false;
                        if (CollectionUtils.isNotEmpty(groupVo.getJobTemplateList())) {
                            hasLaneJob = true;
                            hasGroupJob = true;
                            for (int k = 0; k < groupVo.getJobTemplateList().size(); k++) {
                                PipelineJobTemplateVo jobVo = groupVo.getJobTemplateList().get(k);
                                jobVo.setGroupId(groupVo.getId());
                                pipelineMapper.insertJobTemplate(jobVo);
                            }
                        }
                        if (hasGroupJob) {
                            groupVo.setLaneId(laneVo.getId());
                            groupVo.setSort(j + 1);
                            pipelineMapper.insertLaneGroup(groupVo);
                        }
                    }
                }
                if (hasLaneJob) {
                    laneVo.setPipelineId(pipelineVo.getId());
                    laneVo.setSort(i + 1);
                    pipelineMapper.insertLane(laneVo);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(pipelineVo.getAuthList())) {
            for (PipelineAuthVo authVo : pipelineVo.getAuthList()) {
                authVo.setPipelineId(pipelineVo.getId());
                pipelineMapper.insertPipelineAuth(authVo);
            }
        }
        return pipelineMapper.getPipelineById(pipelineVo.getId());
    }

    public IValid name() {
        return value -> {
            PipelineVo vo = JSONObject.toJavaObject(value, PipelineVo.class);
            if (pipelineMapper.checkPipelineNameIsExists(vo) > 0) {
                return new FieldValidResultVo(new DeployScheduleNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
