/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppPipelineProfileListApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Override
    public String getName() {
        return "获取应用流水线预置参数集列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/List";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值")
    })
    @Output({
            @Param(name = "Return", explode = DeployProfileVo[].class, desc = "应用流水线预置参数集列表")
    })
    @Description(desc = "获取应用流水线预置参数集列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigOverrideVo deployAppConfigOverrideVo = paramObj.toJavaObject(DeployAppConfigOverrideVo.class);
        Long moduleId = deployAppConfigOverrideVo.getModuleId();
        Long envId = deployAppConfigOverrideVo.getEnvId();
        // 如果是访问环境层配置信息，moduleId不能为空
        if (moduleId == 0L && envId != 0L) {
            throw new ParamNotExistsException("moduleId");
        }
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> profileIdList = defaultValue.toJavaList(Long.class);
            DeployPipelineConfigVo config = deployAppPipelineService.getDeployPipelineConfigVo(deployAppConfigOverrideVo, profileIdList);
            return config.getOverrideProfileList();
        } else {
            DeployPipelineConfigVo config = deployAppPipelineService.getDeployPipelineConfigVo(deployAppConfigOverrideVo);
            return config.getOverrideProfileList();
        }
    }
}
