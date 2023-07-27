/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.config.Config;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.crossover.IFileCrossoverService;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Input({
            @Param(name = "filePath", type = ApiParamType.STRING, desc = "文件路径", isRequired = true)
    })
    @Output({
            @Param(name = "result", desc = "内容", type = ApiParamType.STRING),
            @Param(name = "hasMore", desc = "是否有更多内容尚未读取", type = ApiParamType.BOOLEAN)
    })
    @Description(desc = "获取持续集成回调接口调用日志")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Map<String, String> paramMap = new HashMap<>();
        String filePath = paramObj.getString("filePath");
        String[] split = filePath.split("\\?");
        String path = split[0];
        if (split.length >= 2) {
            String[] paramArray = split[1].split("&");
            for (String param : paramArray) {
                if (param.contains("=")) {
                    String[] paramKeyValue = param.split("=");
                    paramMap.put(paramKeyValue[0], paramKeyValue[1]);
                }
            }
        }
        int startIndex = 0;
        int offset = 0;
        int serverId = 0;
        String startIndexStr = paramMap.get("startIndex");
        if (StringUtils.isNotBlank(startIndexStr)) {
            startIndex = Integer.parseInt(startIndexStr);
        }
        String offsetStr = paramMap.get("offset");
        if (StringUtils.isNotBlank(offsetStr)) {
            offset = Integer.parseInt(offsetStr);
        }
        String serverIdStr = paramMap.get("serverId");
        if (StringUtils.isNotBlank(serverIdStr)) {
            serverId = Integer.parseInt(serverIdStr);
        }
        IFileCrossoverService fileCrossoverService = CrossoverServiceFactory.getApi(IFileCrossoverService.class);
        if (Objects.equals(serverId, Config.SCHEDULE_SERVER_ID)) {
            return fileCrossoverService.readLocalFile(path, startIndex, offset);
        } else {
            return fileCrossoverService.readRemoteFile(paramObj, serverId);
        }
    }
}
