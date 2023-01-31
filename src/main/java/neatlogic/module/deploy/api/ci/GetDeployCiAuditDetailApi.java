/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
