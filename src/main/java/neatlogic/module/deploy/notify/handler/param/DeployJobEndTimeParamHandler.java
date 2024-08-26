package neatlogic.module.deploy.notify.handler.param;

import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import neatlogic.framework.util.TimeUtil;
import org.springframework.stereotype.Component;

@Component
public class DeployJobEndTimeParamHandler extends DeployJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null && deployJobVo.getEndTime() != null) {
            return TimeUtil.convertDateToString(deployJobVo.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS);
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.DEPLOYJOBENDTIME.getValue();
    }
}
