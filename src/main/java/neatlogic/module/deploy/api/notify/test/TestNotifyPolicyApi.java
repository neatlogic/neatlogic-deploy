/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        return "测试作业通知策略";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业ID"),
            @Param(name = "jobStatus", type = ApiParamType.STRING, desc = "作业状态")
    })
    @Output({
    })
    @Description(desc = "测试通知策略")
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
