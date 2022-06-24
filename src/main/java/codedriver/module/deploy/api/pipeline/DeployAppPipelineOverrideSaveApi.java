/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigOverrideVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;

//@Service
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
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线部分配置信息")
    })
    @Description(desc = "保存模块或环境对流水线的修改")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigOverrideVo deployAppConfigOverrideVo = paramObj.toJavaObject(DeployAppConfigOverrideVo.class);
        deployAppConfigMapper.insertAppConfigOverride(deployAppConfigOverrideVo);
        return null;
    }
}
