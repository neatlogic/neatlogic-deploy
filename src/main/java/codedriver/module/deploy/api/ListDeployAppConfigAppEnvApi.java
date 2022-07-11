/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author longrf
 * @since 2022/6/17 11:00
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/env/list";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "应用模块id列表"),
            @Param(name = "isHasEnv", type = ApiParamType.INTEGER, desc = "是否拥有环境 (0:查找现没有的环境，1：查找现有的环境)"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "发布应用配置的应用系统环境列表"),
    })
    @Description(desc = "查询发布应用配置的应用系统环境列表(用于应用树的环境下拉、发布作业时通过模块列表查询环境列表)")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray appModuleIdArray = paramObj.getJSONArray("appModuleIdList");
        List<Long> appModuleIdList = null;
        if (CollectionUtils.isNotEmpty(appModuleIdArray)) {
            appModuleIdList = appModuleIdArray.toJavaList(Long.class);
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        List<DeployAppEnvironmentVo> returnEnvList = null;
        if (Objects.nonNull(paramObj.getInteger("isHasEnv")) && paramObj.getInteger("isHasEnv") == 0) {
            returnEnvList = deployAppConfigMapper.getDeployAppHasNotEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList, TenantContext.get().getDataDbName());
        } else {
            returnEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList, TenantContext.get().getDataDbName());
        }
        return returnEnvList;
    }
}
