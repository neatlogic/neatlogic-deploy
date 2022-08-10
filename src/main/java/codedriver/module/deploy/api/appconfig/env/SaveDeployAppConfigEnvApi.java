/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/6/20 10:00 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "保存发布应用配置的应用系统环境";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "环境id列表"),
    })
    @Output({@Param(name = "Return", type = ApiParamType.LONG, desc = "环境id")})
    @Description(desc = "保存发布应用配置的应用系统环境")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray envIdArray = paramObj.getJSONArray("envIdList");
        List<Long> envIdList = null;
        if (CollectionUtils.isNotEmpty(envIdArray)) {
            envIdList = envIdArray.toJavaList(Long.class);
        }

        //校验编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);
        for (Long envId : envIdList) {
            //TODO 循环校验环境权限
        }

        //校验应用系统id、应用模块id、环境id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appModuleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appModuleId"));
        }

        for (Long envId : envIdList) {
            if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(envId) == null) {
                throw new CiEntityNotFoundException(envId);
            }
        }

        deployAppConfigMapper.insertAppConfigEnv(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), envIdList);
        return null;
    }
}
