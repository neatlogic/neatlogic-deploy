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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
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
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.PipelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveDeployAppPipelineDraftApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    PipelineService pipelineService;

    @Override
    public String getName() {
        return "nmdaa.savedeployapppipelinedraftapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/draft/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Description(desc = "nmdaa.savedeployapppipelinedraftapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        if (paramObj.getLong("envId") != null) {
            deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        }

        DeployAppConfigVo deployAppConfigDraftVo = paramObj.toJavaObject(DeployAppConfigVo.class);

        Long appSystemId = deployAppConfigDraftVo.getAppSystemId();
        Long appModuleId = deployAppConfigDraftVo.getAppModuleId();
        Long envId = deployAppConfigDraftVo.getEnvId();
        deployAppConfigDraftVo.setFcu(UserContext.get().getUserUuid());
        deployAppConfigDraftVo.setLcu(UserContext.get().getUserUuid());
        if (appModuleId == 0L && envId == 0L) {
            // 应用层
            DeployAppConfigVo oldDeployAppConfigDraftVo = deployAppConfigMapper.getAppConfigDraft(deployAppConfigDraftVo);
            if (oldDeployAppConfigDraftVo != null) {
                if (Objects.equals(oldDeployAppConfigDraftVo.getConfigStr(), deployAppConfigDraftVo.getConfigStr())) {
                    return null;
                } else {
                    deployAppConfigMapper.updateAppConfigDraft(deployAppConfigDraftVo);
                }
            } else {
                deployAppConfigMapper.insertAppConfigDraft(deployAppConfigDraftVo);
            }
        } else if (envId == 0L) {
            // 模块层
            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigDraft(deployAppConfigDraftVo);
            // 找出修改部分配置
            DeployPipelineConfigVo modifiedPartConfig = pipelineService.getModifiedPartConfig(deployAppConfigDraftVo.getConfig(), null);
            if (modifiedPartConfig == null) {
                if (oldAppModuleAppConfigVo != null) {
                    deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigDraftVo);
                }
                return null;
            }
            deployAppConfigDraftVo.setConfig(modifiedPartConfig);
            if (oldAppModuleAppConfigVo != null) {
                deployAppConfigMapper.updateAppConfigDraft(deployAppConfigDraftVo);
            } else {
                deployAppConfigMapper.insertAppConfigDraft(deployAppConfigDraftVo);
            }
        } else {
            // 环境层
            DeployPipelineConfigVo appModuleAppConfigConfig = null;
            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId));
            if (oldAppModuleAppConfigVo != null) {
                appModuleAppConfigConfig = oldAppModuleAppConfigVo.getConfig();
            }
            DeployAppConfigVo oldAppEnvAppConfigVo = deployAppConfigMapper.getAppConfigDraft(deployAppConfigDraftVo);
            // 找出修改部分配置
            DeployPipelineConfigVo modifiedPartConfig = pipelineService.getModifiedPartConfig(deployAppConfigDraftVo.getConfig(), appModuleAppConfigConfig);
            if (modifiedPartConfig == null) {
                if (oldAppEnvAppConfigVo != null) {
                    deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigDraftVo);
                }
                return null;
            }
            deployAppConfigDraftVo.setConfig(modifiedPartConfig);
            if (oldAppEnvAppConfigVo != null) {
                deployAppConfigMapper.updateAppConfigDraft(deployAppConfigDraftVo);
            } else {
                deployAppConfigMapper.insertAppConfigDraft(deployAppConfigDraftVo);
            }
        }
        return null;
    }
}
