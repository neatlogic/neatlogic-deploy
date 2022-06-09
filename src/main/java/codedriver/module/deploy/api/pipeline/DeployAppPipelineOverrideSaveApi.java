/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopGroupVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigOverrideVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppPipelineOverrideSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/override/save";
    }

    @Override
    public String getName() {
        return "保存模块或环境对流水线的修改";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线部分配置信息")
    })
    @Description(desc = "保存模块或环境对流水线的修改")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigOverrideVo deployAppConfigOverrideVo = paramObj.toJavaObject(DeployAppConfigOverrideVo.class);
//        Long appSystemId = deployAppConfigOverrideVo.getAppSystemId();
//        String appSystemConfigStr = deployAppConfigMapper.getAppConfigByAppSystemId(deployAppConfigOverrideVo.getAppSystemId());
//        if (StringUtils.isNotBlank(appSystemConfigStr)) {
//            throw new DeployAppConfigNotFoundException(appSystemId);
//        }
//        DeployPipelineConfigVo appSystemConfig = JSONObject.parseObject(appSystemConfigStr, DeployPipelineConfigVo.class);
//        Long envId = deployAppConfigOverrideVo.getEnvId();
//        if (envId != 0L) {
//
//        }
//        DeployPipelineConfigVo newConfig = new DeployPipelineConfigVo();
//        DeployPipelineConfigVo config = deployAppConfigOverrideVo.getConfig();
//
//        List<DeployPipelinePhaseVo> deployPipelinePhaseList = config.getCombopPhaseList();
//        if (CollectionUtils.isNotEmpty(deployPipelinePhaseList)) {
//            for (DeployPipelinePhaseVo deployPipelinePhaseVo : deployPipelinePhaseList) {
//                String inherit = deployPipelinePhaseVo.getInherit();
//
//            }
//        }
//        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
//        List<AutoexecProfileVo> overrideProfileList = config.getOverrideProfileList();
//
//        deployAppConfigOverrideVo.setConfig(newConfig);
        deployAppConfigMapper.insertAppConfigOverride(deployAppConfigOverrideVo);
        return null;
    }
}
