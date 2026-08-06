/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigAuditVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class FallbackDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/fallback";
    }

    @Override
    public String getName() {
        return "nmdaae.fallbackdeployappconfigenvautoconfigapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.envid"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "nmdaae.fallbackdeployappconfigenvautoconfigapi.input.param.desc.instanceid"),
            @Param(name = "uuidList", type = ApiParamType.JSONARRAY,  isRequired = true, minSize = 1, desc = "nmdaae.fallbackdeployappconfigenvautoconfigapi.input.param.desc.uuidlist"),
    })
    @Output({
    })
    @Description(desc = "nmdaae.fallbackdeployappconfigenvautoconfigapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        Long instanceId = paramObj.getLong("instanceId");
        if (instanceId == null) {
            instanceId = 0L;
        }
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        List<Long> idList = new ArrayList<>();
        Map<Long, List<String>> id2KeyListMap = new HashMap<>();
        JSONArray uuidList = paramObj.getJSONArray("uuidList");
        for (int i = 0; i < uuidList.size(); i++) {
            String uuid = uuidList.getString(i);
            String[] split = uuid.split("_");
            Long id = Long.parseLong(split[0]);
            idList.add(id);
            id2KeyListMap.computeIfAbsent(id, key -> new ArrayList<>()).add(split[1]);
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            JSONArray jsonArray = new JSONArray();
            Map<String, DeployAppEnvAutoConfigKeyValueVo> oldKeyValueMap = new HashMap<>();
            DeployAppEnvAutoConfigVo appEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, instanceId);
            List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(appEnvAutoConfigVo);
            if (CollectionUtils.isNotEmpty(oldKeyValueList)) {
                oldKeyValueMap = oldKeyValueList.stream().filter(Objects::nonNull).collect(Collectors.toMap(DeployAppEnvAutoConfigKeyValueVo::getKey, e -> e));
            }
            List<DeployAppEnvAutoConfigAuditVo> auditList = deployAppConfigMapper.getAppEnvAutoConfigAuditListByIdList(idList);
            for (DeployAppEnvAutoConfigAuditVo auditVo : auditList) {
                List<String> keyList = id2KeyListMap.get(auditVo.getId());
                if (CollectionUtils.isNotEmpty(keyList)) {
                    JSONObject config = auditVo.getConfig();
                    if (MapUtils.isNotEmpty(config)) {
                        JSONArray tbodyList = config.getJSONArray("tbodyList");
                        if (CollectionUtils.isNotEmpty(tbodyList)) {
                            for (int i = 0; i < tbodyList.size(); i++) {
                                JSONObject tbody = tbodyList.getJSONObject(i);
                                String key = tbody.getString("key");
                                if (keyList.contains(key)) {
                                    Integer isEmpty = tbody.getInteger("beforeIsEmpty");
                                    String type = tbody.getString("beforeType");
                                    String value = tbody.getString("beforeValue");
                                    String action = tbody.getString("action");
                                    if (Objects.equals(action, "insert")) {
                                        DeployAppEnvAutoConfigKeyValueVo keyValueVo = oldKeyValueMap.get(key);
                                        if (keyValueVo != null) {
                                            JSONObject jsonObj = new JSONObject();
                                            jsonObj.put("key", key);
                                            jsonObj.put("beforeType", keyValueVo.getType());
                                            jsonObj.put("beforeValue", keyValueVo.getValue());
                                            jsonObj.put("beforeIsEmpty", keyValueVo.getIsEmpty());
                                            jsonObj.put("action", "delete");
                                            jsonArray.add(jsonObj);
                                            deployAppConfigMapper.deleteAppEnvAutoConfigByKey(appSystemId, appModuleId, envId, instanceId, key);
                                        }
                                    } else if (Objects.equals(action, "delete") || Objects.equals(action, "update")) {
                                        DeployAppEnvAutoConfigKeyValueVo keyValueVo = oldKeyValueMap.get(key);
                                        if (keyValueVo != null) {
                                            JSONObject jsonObj = new JSONObject();
                                            jsonObj.put("key", key);
                                            jsonObj.put("beforeType", keyValueVo.getType());
                                            jsonObj.put("beforeValue", keyValueVo.getValue());
                                            jsonObj.put("beforeIsEmpty", keyValueVo.getIsEmpty());
                                            jsonObj.put("afterType", type);
                                            jsonObj.put("afterValue", value);
                                            jsonObj.put("afterIsEmpty", isEmpty);
                                            jsonObj.put("action", "update");
                                            jsonArray.add(jsonObj);
                                        } else {
                                            JSONObject jsonObj = new JSONObject();
                                            jsonObj.put("key", key);
                                            jsonObj.put("afterType", type);
                                            jsonObj.put("afterValue", value);
                                            jsonObj.put("afterIsEmpty", isEmpty);
                                            jsonObj.put("action", "insert");
                                            jsonArray.add(jsonObj);
                                        }
                                        String lcu = UserContext.get().getUserUuid();
                                        deployAppConfigMapper.insertAppEnvAutoConfigByKey(appSystemId, appModuleId, envId, instanceId, key, type, value, isEmpty, lcu);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(jsonArray)) {
                DeployAppEnvAutoConfigAuditVo deployAppEnvAutoConfigAuditVo = new DeployAppEnvAutoConfigAuditVo();
                deployAppEnvAutoConfigAuditVo.setAppSystemId(appSystemId);
                deployAppEnvAutoConfigAuditVo.setAppModuleId(appModuleId);
                deployAppEnvAutoConfigAuditVo.setEnvId(envId);
                deployAppEnvAutoConfigAuditVo.setInstanceId(instanceId);
                deployAppEnvAutoConfigAuditVo.setConfig(TableResultUtil.getResult(jsonArray));
                deployAppConfigMapper.insertAppEnvAutoConfigAudit(deployAppEnvAutoConfigAuditVo);
            }
        }
        return null;
    }
}
