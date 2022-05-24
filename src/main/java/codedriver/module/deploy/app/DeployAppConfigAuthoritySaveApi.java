/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.app;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author linbq
 * @since 2021/6/16 15:04
 **/
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployAppConfigAuthoritySaveApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/authority/save";
    }

    @Override
    public String getName() {
        return "保存应用配置权限";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appResourceId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "authUuid", type = ApiParamType.STRING, desc = "用户列表"),
            @Param(name = "authType", type = ApiParamType.STRING, desc = "动作列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigAuthorityVo[].class, desc = "应用配置授权列表")
    })
    @Description(desc = "保存应用配置权限")
    @Override
    public Object myDoService(JSONObject paramObj) {
       return null;
    }
}
