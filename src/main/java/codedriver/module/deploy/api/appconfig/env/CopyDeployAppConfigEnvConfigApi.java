/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.deploy.exception.DeployAppConfigNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import codedriver.module.deploy.service.DeployAppConfigService;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/8/29 17:11
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CopyDeployAppConfigEnvConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "复制环境层配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/config/copy";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "fromEnvId", type = ApiParamType.LONG, isRequired = true, desc = "来源环境id"),
            @Param(name = "toEnvIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "目标环境id列表"),
            @Param(name = "isAdd", type = ApiParamType.INTEGER, isRequired = true, desc = "是否新增(0:现有，1:新增)")
    })
    @Output({
    })
    @Description(desc = "复制环境层配置(复制环境层配置)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long fromEnvId = paramObj.getLong("fromEnvId");
        JSONArray toEnvIdArray = paramObj.getJSONArray("toEnvIdList");
        List<Long> toEnvIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toEnvIdArray)) {
            toEnvIdList = toEnvIdArray.toJavaList(Long.class);
        }

        //校验编辑配置的操作权限、环境权限操作
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        List<Long> checkAuthEnvIdList = new ArrayList<>(toEnvIdList);
        checkAuthEnvIdList.add(fromEnvId);
        deployAppAuthorityService.checkEnvAuthList(appSystemId, checkAuthEnvIdList);

        //来源环境层独有一份配置
        boolean hasFromEnvConfig = deployAppConfigMapper.getAppConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, fromEnvId) != null;

        //复制流水线配置
        DeployPipelineConfigVo fromEnvConfigVo = DeployPipelineConfigManager.init(appSystemId).withAppModuleId(appModuleId).withEnvId(fromEnvId).getConfig();
        if (fromEnvConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appModuleId);
        }
        List<DeployAppConfigVo> insertConfigList = new ArrayList<>();
        for (Long envId : toEnvIdList) {
            if (hasFromEnvConfig) {
                insertConfigList.add(new DeployAppConfigVo(appSystemId, appModuleId, envId, fromEnvConfigVo));
            } else {
                deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, appModuleId, envId));
            }
        }
        if (CollectionUtils.isNotEmpty(insertConfigList)) {
            deployAppConfigMapper.insertBatchAppConfig(insertConfigList);
        }

        //新增环境
        if (paramObj.getInteger("isAdd") == 1) {
            deployAppConfigMapper.insertAppConfigEnv(appSystemId, appModuleId, toEnvIdList);
        }

        //复制dbConfig和autoConfig
        copyDbSchemaListAndAutoCfgKeyList(appSystemId, appModuleId, fromEnvId, toEnvIdList);

        return null;
    }


    /**
     * 复制dbSchemas和autoCfgKeys配置
     *
     * @param appSystemId 系统id
     * @param appModuleId 模块id
     * @param fromEnvId   来源环境id
     * @param toEnvIdList 目标环境id列表
     */
    private void copyDbSchemaListAndAutoCfgKeyList(Long appSystemId, Long appModuleId, Long fromEnvId, List<Long> toEnvIdList) {
        if (CollectionUtils.isEmpty(toEnvIdList)) {
            return;
        }

        DeployAppEnvironmentVo envInfoVo = deployAppConfigMapper.getAppConfigEnvIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, fromEnvId, TenantContext.get().getDataDbName());
        if (envInfoVo == null) {
            return;
        }
        List<DeployAppConfigEnvDBConfigVo> insertDbConfigVoList = new ArrayList<>();
        List<DeployAppEnvAutoConfigVo> insertAutoCfgVoList = new ArrayList<>();

        for (Long envId : toEnvIdList) {

            //删除dbConfig和autoConfig
            deployAppConfigMapper.deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envId);
            deployAppConfigMapper.deleteAppConfigDBConfig(new DeployAppConfigEnvDBConfigVo(appSystemId, appModuleId, envId));

            //构造dbSchema
            if (CollectionUtils.isNotEmpty(envInfoVo.getDbSchemaList())) {
                for (DeployAppConfigEnvDBConfigVo dbConfigVo : envInfoVo.getDbSchemaList()) {
                    insertDbConfigVoList.add(new DeployAppConfigEnvDBConfigVo(appSystemId, appModuleId, envId, dbConfigVo.getDbSchema(), dbConfigVo.getConfig()));
                }
            }

            //构造autoCfgKeyValue
            if (CollectionUtils.isNotEmpty(envInfoVo.getAutoCfgKeyValueList())) {
                insertAutoCfgVoList.add(new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, envInfoVo.getAutoCfgKeyValueList()));
            }
        }

        //批量新增dbConfig和autoConfig
        if (CollectionUtils.isNotEmpty(insertDbConfigVoList)) {
            deployAppConfigMapper.insertBatchAppConfigEnvDBConfig(insertDbConfigVoList);
        }
        if (CollectionUtils.isNotEmpty(insertAutoCfgVoList)) {
            deployAppConfigMapper.insertBatchAppEnvAutoConfig(insertAutoCfgVoList);
        }
    }
}
