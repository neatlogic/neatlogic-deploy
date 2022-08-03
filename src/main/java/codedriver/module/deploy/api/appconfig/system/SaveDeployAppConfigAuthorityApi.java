/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.dto.AuthorityVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAuthorityApi extends PrivateApiComponentBase {
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "authorityStrList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "授权列表"),
            @Param(name = "actionList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "授权操作列表"),
            @Param(name = "isEdit", type = ApiParamType.INTEGER, isRequired = true, desc = "是否编辑，0：否，1：是"),
    })
    @Output({
    })
    @Description(desc = "保存应用配置权限")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployAppConfigAuthorityVo deployAppConfigAuthorityVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);
        Date nowTime = new Date(System.currentTimeMillis());
        deployAppConfigAuthorityVo.setLcd(nowTime);
        for (AuthorityVo authorityVo : deployAppConfigAuthorityVo.getAuthorityList()) {
            deployAppConfigAuthorityVo.setAuthUuid(authorityVo.getUuid());
            deployAppConfigAuthorityVo.setAuthType(authorityVo.getType());
            deployAppConfigMapper.insertAppConfigAuthority(deployAppConfigAuthorityVo);
        }

        //如果是编辑，则需要删除多余权限
        if (deployAppConfigAuthorityVo.getIsEdit() == 1) {
            AuthorityVo authorityVo = deployAppConfigAuthorityVo.getAuthorityList().get(0);
            deployAppConfigMapper.deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(deployAppConfigAuthorityVo.getAppSystemId(), deployAppConfigAuthorityVo.getEnvId(), authorityVo.getUuid(), nowTime);
        }
        return null;
    }
}
