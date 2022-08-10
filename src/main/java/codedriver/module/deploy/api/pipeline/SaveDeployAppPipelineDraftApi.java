/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
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

    @Override
    public String getName() {
        return "保存流水线草稿";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线草稿配置信息")
    })
    @Description(desc = "保存流水线草稿")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        if (!Objects.isNull(paramObj.getLong("envId"))) {
            deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        }

        DeployAppConfigVo deployAppConfigDraftVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        DeployAppConfigVo oldDeployAppConfigDraftVo = deployAppConfigMapper.getAppConfigDraft(deployAppConfigDraftVo);
        if (oldDeployAppConfigDraftVo != null) {
            if (Objects.equals(oldDeployAppConfigDraftVo.getConfigStr(), deployAppConfigDraftVo.getConfigStr())) {
                return null;
            }
            deployAppConfigDraftVo.setLcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.updateAppConfigDraft(deployAppConfigDraftVo);
        } else {
            deployAppConfigDraftVo.setFcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.insertAppConfigDraft(deployAppConfigDraftVo);
        }
        return null;
    }
}
