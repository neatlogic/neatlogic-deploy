/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeleteBatchDeployAppConfigAuthorityApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/authority/batch/delete";
    }

    @Override
    public String getName() {
        return "批量删除应用配置权限";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "authorityVoList", type = ApiParamType.JSONARRAY, explode = DeployAppConfigAuthorityVo[].class, desc = "需要删除的应用权限列表列表")
    })
    @Output({
    })
    @Description(desc = "批量删除应用配置权限")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray authorityArray = paramObj.getJSONArray("authorityVoList");
        List<DeployAppConfigAuthorityVo> authorityVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(authorityArray)) {
            authorityVoList = authorityArray.toJavaList(DeployAppConfigAuthorityVo.class);
        }
        if (CollectionUtils.isNotEmpty(authorityVoList)) {
            for (DeployAppConfigAuthorityVo deployAppConfigAuthorityVo : authorityVoList) {
                deployAppConfigMapper.deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(deployAppConfigAuthorityVo.getAppSystemId(), deployAppConfigAuthorityVo.getEnvId(), deployAppConfigAuthorityVo.getAuthUuid(), null);
            }
        }
        return null;
    }
}
