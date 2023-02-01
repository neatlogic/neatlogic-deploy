/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.exception.file.FilePathIllegalException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.AuditUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployCiAuditDetailApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "deploy/ci/audit/detail/get";
    }

    @Override
    public String getName() {
        return "获取持续集成回调接口调用日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "filePath", type = ApiParamType.STRING, desc = "调用记录文件路径", isRequired = true)})
    @Output({
            @Param(name = "result", desc = "内容", type = ApiParamType.STRING),
            @Param(name = "hasMore", desc = "是否有更多内容尚未读取", type = ApiParamType.BOOLEAN)
    })
    @Description(desc = "获取持续集成回调接口调用日志")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        JSONObject resultJson = new JSONObject();
        String filePath = jsonObj.getString("filePath");
        if (!filePath.contains("?") || !filePath.contains("&") || !filePath.contains("=")) {
            throw new FilePathIllegalException(filePath);
        }
        long offset = Long.parseLong(filePath.split("\\?")[1].split("&")[1].split("=")[1]);
        resultJson.put("result", AuditUtil.getAuditDetail(filePath));
        if (offset > AuditUtil.maxFileSize) {
            resultJson.put("hasMore", true);
        } else {
            resultJson.put("hasMore", false);
        }
        return resultJson;
    }
}
