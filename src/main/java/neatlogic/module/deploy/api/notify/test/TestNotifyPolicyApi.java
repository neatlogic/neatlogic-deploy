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

package neatlogic.module.deploy.api.notify.test;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.NOTIFY_POLICY_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackFactory;
import neatlogic.framework.autoexec.job.callback.core.IAutoexecJobCallback;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

@Service
@AuthAction(action = NOTIFY_POLICY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestNotifyPolicyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmdant.testnotifypolicyapi.getname";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.jobid"),
            @Param(name = "jobStatus", type = ApiParamType.STRING, desc = "term.autoexec.jobstatuslabel")
    })
    @Output({
    })
    @Description(desc = "nmdant.testnotifypolicyapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String jobStatus = paramObj.getString("jobStatus");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (StringUtils.isNotBlank(jobStatus)) {
            jobVo.setStatus(jobStatus);
        }
        Map<String, IAutoexecJobCallback> handlerMap = AutoexecJobCallbackFactory.getHandlerMap();
        for (Map.Entry<String, IAutoexecJobCallback> entry : handlerMap.entrySet()) {
            String key = entry.getKey();
            if (Objects.equals(key, "DeployJobNotifyCallbackHandler") || Objects.equals(key, "AutoexecJobNotifyCallbackHandler")) {
                entry.getValue().doService(null, jobVo);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/notify/policy/test";
    }
}
