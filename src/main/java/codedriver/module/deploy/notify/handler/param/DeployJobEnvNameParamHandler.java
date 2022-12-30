/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.notify.handler.param;

import codedriver.framework.deploy.constvalue.DeployJobNotifyParam;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

/**
 * @author longrf
 * @date 2022/12/29 14:22
 */
@Component
public class DeployJobEnvNameParamHandler extends DeployJobNotifyParamHandlerBase {

    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return deployJobVo.getEnvName();
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.ENVNAME.getValue();
    }
}
