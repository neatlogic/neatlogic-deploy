/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.api.appconfig.module;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployRunnerGroupForAutoexecApi extends PrivateApiComponentBase {
    @Resource
    DeployAppConfigMapper deployAppConfigMapper;


    @Override
    public String getName() {
        return "获取发布runner组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/runner/group/get/forautoexec";
    }

    @Input({
            @Param(name = "sysId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id")
    })
    @Output({@Param(type = ApiParamType.JSONOBJECT)})
    @Example(example = "{\"1\": \"192.168.1.140\"}")
    @Description(desc = "获取发布runner组接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long sysId = jsonObj.getLong("sysId");
        Long moduleId = jsonObj.getLong("moduleId");
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(sysId, moduleId);
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(sysId.toString(), moduleId.toString());
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        for (RunnerMapVo runnerMapVo : runnerGroupVo.getRunnerMapList()) {
            result.put(runnerMapVo.getRunnerMapId().toString(), runnerMapVo.getHost());
        }
        return result;
    }

}
