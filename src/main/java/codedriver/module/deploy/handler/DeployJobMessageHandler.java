/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.handler;

import codedriver.framework.message.core.MessageHandlerBase;
import codedriver.framework.notify.dto.NotifyVo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/12/29 16:06
 */
@Service
public class DeployJobMessageHandler extends MessageHandlerBase {

    @Override
    public String getName() {
        return "发布作业";
    }

    @Override
    public String getDescription() {
        return "实时显示发布作业相关信息";
    }

    @Override
    public boolean getNeedCompression() {
        return false;
    }

    @Override
    public NotifyVo compress(List<NotifyVo> notifyVoList) {
        return null;
    }
}
