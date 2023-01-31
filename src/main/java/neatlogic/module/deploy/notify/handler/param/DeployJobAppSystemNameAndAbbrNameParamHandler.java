/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.notify.handler.param;

import neatlogic.framework.deploy.constvalue.DeployJobNotifyParam;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.notify.DeployJobNotifyParamHandlerBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author longrf
 * @date 2022/12/29 12:06
 */
@Component
public class DeployJobAppSystemNameAndAbbrNameParamHandler extends DeployJobNotifyParamHandlerBase {
    @Override
    public Object getMyText(DeployJobVo deployJobVo) {
        if (deployJobVo != null) {
            return deployJobVo.getAppSystemAbbrName() + (StringUtils.isNotEmpty(deployJobVo.getAppSystemName()) ? "(" + deployJobVo.getAppSystemName() + ")" : "");
        }
        return null;
    }

    @Override
    public String getValue() {
        return DeployJobNotifyParam.APPSYSTEMNAMEANDABBRNAME.getValue();
    }
}
