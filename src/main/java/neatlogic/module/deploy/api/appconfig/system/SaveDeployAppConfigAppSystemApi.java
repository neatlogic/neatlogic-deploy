/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.api.appconfig.system;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.APP_CONFIG_MODIFY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.DeployAppConfigActionType;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityActionVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import neatlogic.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/14 4:17 下午
 */
@Service
@Transactional
@AuthAction(action = APP_CONFIG_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployAppConfigAppSystemApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "nmdaas.savedeployappconfigappsystemapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "abbrName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.sysname"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name"),
            @Param(name = "state", type = ApiParamType.JSONARRAY, desc = "common.status"),
            @Param(name = "owner", type = ApiParamType.JSONARRAY, desc = "common.maintenanceman"),
            @Param(name = "maintenanceWindow", type = ApiParamType.STRING, desc = "term.cmdb.maintenancewindow"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description")
    })
    @Output({@Param(name = "Return", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid")})
    @Description(desc = "nmdaas.savedeployappconfigappsystemapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long appSystemId = paramObj.getLong("id");

        //校验编辑配置的操作权限
        if (appSystemId != null) {
            deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        }

        List<Long> stateIdList = new ArrayList<>();
        List<Long> ownerIdList = new ArrayList<>();
        //构建数据结构
        JSONArray stateIdArray = paramObj.getJSONArray("state");
        if (CollectionUtils.isNotEmpty(stateIdArray)) {
            stateIdList = stateIdArray.toJavaList(Long.class);
        }
        JSONArray ownerIdArray = paramObj.getJSONArray("owner");
        if (CollectionUtils.isNotEmpty(ownerIdArray)) {
            ownerIdList = ownerIdArray.toJavaList(Long.class);
        }
        paramObj.put("stateIdList", stateIdList);
        paramObj.put("ownerIdList", ownerIdList);

        //定义需要插入的字段
        List<String> needUpdateAttrList = Arrays.asList("state", "name", "owner", "abbrName", "maintenance_window", "description");
        //获取应用系统的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APP");

        //保存
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityTransactionVo ciEntityTransactionVo = null;
        if (appSystemId == null) {

            /*新增应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            ciEntityTransactionVo = new CiEntityTransactionVo();
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, appCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {

            /*编辑应用系统（配置项）*/
            //1、构建事务vo，并添加属性值
            CiEntityVo systemCiEntityInfo = ciEntityService.getCiEntityById(appCiVo.getId(), appSystemId);
            if (systemCiEntityInfo == null) {
                throw new CiEntityNotFoundException(appSystemId);
            }

            ciEntityTransactionVo = new CiEntityTransactionVo(systemCiEntityInfo);
            ciEntityTransactionVo.setAttrEntityData(systemCiEntityInfo.getAttrEntityData());
            deployAppConfigService.addAttrEntityDataAndRelEntityData(ciEntityTransactionVo, appCiVo.getId(), paramObj, needUpdateAttrList, new ArrayList<>());

            //2、设置事务vo信息
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        }
        //3、保存系统（配置项）
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        ciEntityTransactionList.add(ciEntityTransactionVo);
        ciEntityService.saveCiEntity(ciEntityTransactionList);

        // 添加应用时，给创建用户添加全部的操作权限，包括查看作业/配置、编辑配置、版本&制品管理、超级流水线权限
        if (appSystemId == null) {
            DeployAppConfigAuthorityVo deployAppConfigAuthorityVo = new DeployAppConfigAuthorityVo();
            deployAppConfigAuthorityVo.setAppSystemId(ciEntityTransactionVo.getCiEntityId());
            deployAppConfigAuthorityVo.setAuthType(GroupSearch.USER.getValue());
            deployAppConfigAuthorityVo.setAuthUuid(UserContext.get().getUserUuid());
            deployAppConfigAuthorityVo.setLcd(new Date());
            deployAppConfigAuthorityVo.setLcu(UserContext.get().getUserUuid());
            List<DeployAppConfigAuthorityActionVo> actionList = new ArrayList<>();
            for (DeployAppConfigAction deployAppConfigAction : DeployAppConfigAction.values()) {
                actionList.add(new DeployAppConfigAuthorityActionVo(deployAppConfigAction.getValue(), DeployAppConfigActionType.OPERATION.getValue()));
            }
            deployAppConfigAuthorityVo.setActionList(actionList);
            deployAppConfigMapper.insertAppConfigAuthority(deployAppConfigAuthorityVo);
        }
        return ciEntityTransactionVo.getCiEntityId();
    }
}
