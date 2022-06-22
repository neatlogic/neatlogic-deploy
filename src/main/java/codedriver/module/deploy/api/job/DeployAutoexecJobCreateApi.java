/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class DeployAutoexecJobCreateApi extends PrivateApiComponentBase {


    @Override
    public String getName() {
        return "作业创建（来自组合工具）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源 itsm|human   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "来源id"),

            @Param(name = "threadCount", type = ApiParamType.LONG, isRequired = true, desc = "并发线程,2的n次方 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
    })
    @Output({
    })
    @Description(desc = "作业创建（来自组合工具）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        AutoexecJobVo jobVo = null;
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setIsFirstFire(1);
        fireAction.doService(jobVo);
        return new JSONObject() {{
            put("jobId", jobVo.getId());
        }};
    }

    @Override
    public String getToken() {
        return "/deploy/autoexec/job/create";
    }
}
