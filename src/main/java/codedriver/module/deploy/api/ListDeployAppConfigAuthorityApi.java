/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api;

import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2022/6/15 11:04
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAuthorityApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/deploy/app/config/authority/list";
    }

    @Override
    public String getName() {
        return "查询应用系统权限列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
    })
    @Description(desc = "查询应用系统权限列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        //TODO 场景
        return DeployAppConfigAction.getValueTextList();
    }
}
