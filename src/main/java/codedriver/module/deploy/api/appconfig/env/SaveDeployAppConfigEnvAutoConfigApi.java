/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/save";
    }

    @Override
    public String getName() {
        return "保存应用环境实例autoConfig";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
            @Param(name = "deleteInstanceId", type = ApiParamType.LONG, desc = "删除的应用实例 id"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "应用实例 id"),
            @Param(name = "keyValueList", type = ApiParamType.JSONARRAY, desc = "[{\"id\": xxx,\"key\": xxx,\"value\":xxx}]"),
    })
    @Output({
    })
    @Description(desc = "保存应用环境实例autoConfig接口")
    @Override
    public Object myDoService(JSONObject paramObj) {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSONObject.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        Date nowDate = new Date(System.currentTimeMillis());
        appEnvAutoConfigVo.setLcd(nowDate);
        if (CollectionUtils.isNotEmpty(appEnvAutoConfigVo.getKeyValueList())) {
            deployAppConfigMapper.insertAppEnvAutoConfig(appEnvAutoConfigVo);
        }
        deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
        Long deleteInstanceId = paramObj.getLong("deleteInstanceId");
        if (!Objects.isNull(deleteInstanceId)) {
            DeployAppEnvAutoConfigVo deleteAppEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), deleteInstanceId);
            deployAppConfigMapper.deleteAppEnvAutoConfig(deleteAppEnvAutoConfigVo);
        }
        return null;
    }
}
