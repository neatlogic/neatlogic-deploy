/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.exception.DeployAppConfigNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        //来源环境层独有一份配置
        boolean hasFromEnvConfig = deployAppConfigMapper.getAppConfigByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, fromEnvId) != null;

        //复制流水线配置
        DeployPipelineConfigVo fromEnvConfigVo = DeployPipelineConfigManager.init(appSystemId).withAppModuleId(appModuleId).withEnvId(fromEnvId).getConfig();
        if (fromEnvConfigVo == null) {
            throw new DeployAppConfigNotFoundException(appModuleId);
        }
        List<DeployAppConfigVo> insertAppConfigList = new ArrayList<>();
        for (Long envId : toEnvIdList) {
            if (hasFromEnvConfig) {
                insertAppConfigList.add(new DeployAppConfigVo(appSystemId, appModuleId, envId, fromEnvConfigVo));
            } else {
                //删除原有的配置(虽然前端已经有接口控制只能选没有独一份配置的环境，但是防止单独调接口，做多一次删除动作)
                deployAppConfigMapper.deleteAppConfig(new DeployAppConfigVo(appSystemId, appModuleId, envId));
            }
        }
        if (CollectionUtils.isNotEmpty(insertAppConfigList)) {
            deployAppConfigMapper.insertBatchAppConfig(insertAppConfigList);
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

        List<DeployAppEnvironmentVo> envInfoVoList = deployAppConfigMapper.getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, Collections.singletonList(fromEnvId));
        if (CollectionUtils.isEmpty(envInfoVoList)) {
            return;
        }
        DeployAppEnvironmentVo envInfoVo = envInfoVoList.get(0);
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
