/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.type;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployTypeMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/12/8 14:55
 */

@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateSaveDeployTypeStatusApi extends PrivateApiComponentBase {

    @Resource
    DeployTypeMapper deployTypeMapper;

    @Resource
    AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "激活/禁用发布工具类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/type/status/update";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "工具类型D"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "是否激活(0:禁用，1：激活)"),
    })
    @Output({
    })
    @Description(desc = "激活/禁用发布工具类型")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (autoexecTypeMapper.checkTypeIsExistsById(id) == 0) {
            throw new AutoexecTypeNotFoundException(id);
        }
        deployTypeMapper.insertTypeActive(paramObj.getInteger("isActive"), id);
        return null;
    }
}
