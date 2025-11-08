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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dependency.handler.Matrix2DeployAppPipelineParamDependencyHandler;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppPipelineParamApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/param/save";
    }

    @Override
    public String getName() {
        return "保存应用流水线作业参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "参数列表[{\"key\": \"参数名\", \"name\": \"中文名\", \"defaultValue\": \"默认值\", \"description\": \"描述\", \"isRequired\": \"是否必填\", \"type\": \"参数类型\"}]")
    })
    @Description(desc = "保存应用流水线作业参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(jsonObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        Long appSystemId = jsonObj.getLong("appSystemId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo != null) {
            DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
            if (config != null) {
                List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
                if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                    for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
                        DependencyManager.delete(Matrix2DeployAppPipelineParamDependencyHandler.class, autoexecParamVo.getId());
                    }
                }
            }
        }
        List<AutoexecParamVo> runtimeParamList = new ArrayList<>();
        JSONArray paramList = jsonObj.getJSONArray("paramList");
        if (CollectionUtils.isNotEmpty(paramList)) {
            runtimeParamList = paramList.toJavaList(AutoexecParamVo.class);
            IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
            autoexecServiceCrossoverService.validateRuntimeParamList(runtimeParamList);
            for (int i = 0; i < runtimeParamList.size(); i++) {
                AutoexecParamVo autoexecParamVo = runtimeParamList.get(i);
                if (autoexecParamVo != null) {
                    String type = autoexecParamVo.getType();
                    ParamType paramType = ParamType.getParamType(type);
                    // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                    if (paramType == ParamType.PASSWORD) {
                        if (autoexecParamVo.getDefaultValue() != null) {
                            String value = autoexecParamVo.getDefaultValue().toString();
                            if (StringUtils.isNotBlank(value)) {
                                autoexecParamVo.setDefaultValue(RC4Util.encrypt(value));
                            }
                        }
                    } else if (paramType == ParamType.SELECT || paramType == ParamType.MULTISELECT || paramType == ParamType.CHECKBOX || paramType == ParamType.RADIO) {
                        AutoexecParamConfigVo config = autoexecParamVo.getConfig();
                        if (config != null) {
                            String matrixUuid = config.getMatrixUuid();
                            if (StringUtils.isNotBlank(matrixUuid)) {
                                JSONObject dependencyConfig = new JSONObject();
                                dependencyConfig.put("appSystemId", appSystemId);
                                DependencyManager.insert(Matrix2DeployAppPipelineParamDependencyHandler.class, matrixUuid, autoexecParamVo.getId(), dependencyConfig);
                            }
                        }
                    }
                    autoexecParamVo.setSort(i);
                }
            }
        }

        if (deployAppConfigVo == null) {
            deployAppConfigVo = new DeployAppConfigVo(appSystemId);
            DeployPipelineConfigVo config = new DeployPipelineConfigVo();
            config.setRuntimeParamList(runtimeParamList);
            deployAppConfigVo.setConfig(config);
            deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
        } else {
            DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
            if (config == null) {
                config = new DeployPipelineConfigVo();
                deployAppConfigVo.setConfig(config);
            }
            config.setRuntimeParamList(runtimeParamList);
            deployAppConfigVo.setConfigStr(null);
            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
        }
        return null;
    }

}
