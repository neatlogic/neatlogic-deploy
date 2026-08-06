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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.pipeline.*;
import neatlogic.framework.deploy.exception.DeployPipelineNotFoundException;
import neatlogic.framework.deploy.exception.DeployScheduleNameRepeatException;
import neatlogic.framework.deploy.exception.pipeline.DeployPipelineParamCannotBeEmptyException;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SavePipelineApi extends PrivateApiComponentBase {
    @Resource
    private DeployPipelineMapper pipelineMapper;
    @Resource
    private DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdap.savepipelineapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/pipeline/save";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "nmdap.savepipelineapi.input.param.desc.id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmdap.savepipelineapi.input.param.desc.name"),
            @Param(name = "type", type = ApiParamType.ENUM, member = PipelineType.class, isRequired = true, desc = "common.type"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "nmraa.getappapi.input.param.desc"),
            @Param(name = "laneList", type = ApiParamType.JSONARRAY, desc = "term.deploy.lanelist"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "common.authlist"),
            @Param(name = "defaultVersion", type = ApiParamType.STRING, desc = "nmdap.savepipelineapi.input.param.desc.defaultversion"),
        }
    )
    @ResubmitInterval(3)
    @Output({@Param(explode = PipelineVo.class)})
    @Description(desc = "nmdap.savepipelineapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (id != null) {
            PipelineVo pipelineVo = pipelineMapper.getPipelineById(id);
            if (pipelineVo == null) {
                throw new DeployPipelineNotFoundException(id);
            }
        }
        PipelineVo pipelineVo = JSON.toJavaObject(jsonObj, PipelineVo.class);
        String type = pipelineVo.getType();
        if (Objects.equals(type, PipelineType.GLOBAL.getValue())) {
            if (Boolean.FALSE.equals(AuthActionChecker.check(PIPELINE_MODIFY.class))) {
                throw new PermissionDeniedException(PIPELINE_MODIFY.class);
            }
        } else if (Objects.equals(type, PipelineType.APPSYSTEM.getValue())) {
            deployAppAuthorityService.checkOperationAuth(pipelineVo.getAppSystemId(), DeployAppConfigAction.PIPELINE);
        }
        verifyPipelineConfig(pipelineVo);
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
                                jobVo.setSort(k);
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

    private void verifyPipelineConfig(PipelineVo pipelineVo) {
        List<Long> appSystemIdList = new ArrayList<>();
        List<PipelineLaneVo> laneList = pipelineVo.getLaneList();
        if (CollectionUtils.isNotEmpty(laneList)) {
            for (PipelineLaneVo pipelineLaneVo : laneList) {
                List<PipelineGroupVo> groupList = pipelineLaneVo.getGroupList();
                if (CollectionUtils.isNotEmpty(groupList)) {
                    for (PipelineGroupVo pipelineGroupVo : groupList) {
                        List<PipelineJobTemplateVo> jobTemplateList = pipelineGroupVo.getJobTemplateList();
                        if (CollectionUtils.isNotEmpty(jobTemplateList)) {
                            for (PipelineJobTemplateVo pipelineJobTemplateVo : jobTemplateList) {
                                Long appSystemId = pipelineJobTemplateVo.getAppSystemId();
                                if (appSystemId != null && !appSystemIdList.contains(appSystemId)) {
                                    appSystemIdList.add(appSystemId);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            Map<Long, List<AutoexecParamVo>> appSystemId2RuntimeParamListMap = new HashMap<>();
            List<DeployAppConfigVo> appConfigList = deployAppConfigMapper.getAppConfigListByAppSystemIdList(appSystemIdList);
            for (DeployAppConfigVo deployAppConfigVo : appConfigList) {
                DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
                if (config != null && CollectionUtils.isNotEmpty(config.getRuntimeParamList())) {
                    appSystemId2RuntimeParamListMap.put(deployAppConfigVo.getAppSystemId(), config.getRuntimeParamList());
                }
            }
            if (CollectionUtils.isNotEmpty(laneList)) {
                for (int i = 0; i < laneList.size(); i++) {
                    PipelineLaneVo laneVo = laneList.get(i);
                    List<PipelineGroupVo> groupList = laneVo.getGroupList();
                    if (CollectionUtils.isNotEmpty(groupList)) {
                        for (int j = 0; j < groupList.size(); j++) {
                            PipelineGroupVo groupVo = groupList.get(j);
                            List<PipelineJobTemplateVo> jobTemplateList = groupVo.getJobTemplateList();
                            if (CollectionUtils.isNotEmpty(jobTemplateList)) {
                                for (int k = 0; k < jobTemplateList.size(); k++) {
                                    PipelineJobTemplateVo jobVo = jobTemplateList.get(k);
                                    Long appSystemId = jobVo.getAppSystemId();
                                    List<AutoexecParamVo> runtimeParamList = appSystemId2RuntimeParamListMap.get(appSystemId);
                                    if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                                        JSONObject param = null;
                                        JSONObject config = jobVo.getConfig();
                                        if (MapUtils.isNotEmpty(config)) {
                                            param = config.getJSONObject("param");
                                        }
                                        if (param == null) {
                                            param = new JSONObject();
                                        }
                                        for (AutoexecParamVo runtimeParam : runtimeParamList) {
                                            if (Objects.equals(runtimeParam.getIsRequired(), 1)) {
                                                if (!param.containsKey(runtimeParam.getKey())) {
                                                    throw new DeployPipelineParamCannotBeEmptyException(i + 1, j + 1, k + 1, runtimeParam.getName(), runtimeParam.getKey());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
