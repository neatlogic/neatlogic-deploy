package codedriver.module.deploy.notify.handler.param;


import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.deploy.constvalue.DeployJobNotifyParam;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DeployJobPhaseListParamHandler extends DeployJobNotifyParamHandlerBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return autoexecJobMapper.getJobPhaseListByJobId(deployJobVo.getId());
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.JOBPHASELIST.getValue();
    }
}
