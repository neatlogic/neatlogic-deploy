package codedriver.module.deploy.notify.handler.param;

import codedriver.framework.deploy.constvalue.DeployJobNotifyParam;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

@Component
public class DeployJobStatusParamHandler extends DeployJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return deployJobVo.getStatus();
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.JOBSTATUS.getValue();
    }
}
