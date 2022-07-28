/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.callback;

import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;

/**
 * @author lvzk
 * @since 2022/7/27 17:40
 **/
public class BatchJobCallbackHandler extends AutoexecJobCallbackBase {
    @Override
    public String getHandler() {
        return null;
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        return null;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {

    }
}
