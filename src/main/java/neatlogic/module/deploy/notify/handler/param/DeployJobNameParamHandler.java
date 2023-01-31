package neatlogic.module.deploy.notify.handler.param;

import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

@Component
public class DeployJobNameParamHandler extends DeployJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(DeployJobVo DeployJobVo) {
        if (DeployJobVo != null) {
            return DeployJobVo.getName();
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.JOBNAME.getValue();
    }
}
