/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.audit;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.crossover.ICrossoverService;
import neatlogic.framework.deploy.dto.ci.DeployCiAuditVo;
import neatlogic.framework.file.core.IEvent;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Component
public class DeployCiCallbackAuditAppendPostProcessor implements Consumer<IEvent>, ICrossoverService {
    @Resource
    private DeployCiMapper deployCiMapper;

    @Override
    public void accept(IEvent event) {
        JSONObject data = event.getData();
        String path = data.getString("path");
        String dataHome = Config.DATA_HOME() + TenantContext.get().getTenantUuid();
        if (path.startsWith(dataHome)) {
            path = "${home}" + path.substring(dataHome.length());
        }
        JSONObject deployCiAudit = data.getJSONObject("deployCiAudit");
        DeployCiAuditVo deployCiAuditVo = deployCiAudit.toJavaObject(DeployCiAuditVo.class);
        long fileSize = event.getBeforeAppendFileSize();
        String message = event.getFormattedMessage();
        String param = deployCiAuditVo.getParam();
        String result = deployCiAuditVo.getResult() != null ? deployCiAuditVo.getResult().toString() : null;
        String error = deployCiAuditVo.getError();
        if (StringUtils.isNotBlank(param)) {
            int index = message.indexOf(param);
            int startIndex = message.substring(0, index).getBytes(StandardCharsets.UTF_8).length;
            int offset = param.getBytes(StandardCharsets.UTF_8).length;
            String filePath = path + "?startIndex=" + (fileSize + startIndex) + "&offset=" + offset + "&serverId=" + Config.SCHEDULE_SERVER_ID;
            deployCiAuditVo.setParamFilePath(filePath);
        }
        if (StringUtils.isNotBlank(result)) {
            int index = message.indexOf(result);
            int startIndex = message.substring(0, index).getBytes(StandardCharsets.UTF_8).length;
            int offset = result.getBytes(StandardCharsets.UTF_8).length;
            String filePath = path + "?startIndex=" + (fileSize + startIndex) + "&offset=" + offset + "&serverId=" + Config.SCHEDULE_SERVER_ID;
            deployCiAuditVo.setResultFilePath(filePath);
        }
        if (StringUtils.isNotBlank(error)) {
            int index = message.indexOf(error);
            int startIndex = message.substring(0, index).getBytes(StandardCharsets.UTF_8).length;
            int offset = error.getBytes(StandardCharsets.UTF_8).length;
            String filePath = path + "?startIndex=" + (fileSize + startIndex) + "&offset=" + offset + "&serverId=" + Config.SCHEDULE_SERVER_ID;
            deployCiAuditVo.setErrorFilePath(filePath);
        }
        deployCiMapper.insertDeployCiAudit(deployCiAuditVo);
    }
}
