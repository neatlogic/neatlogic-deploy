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
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigAuditVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyIrregularException;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyRepeatException;
import neatlogic.framework.deploy.exception.DeployAppConfigEnvAutoConfigKeyTypeIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
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
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        Long instanceId = paramObj.getLong("instanceId");
        if (instanceId == null) {
            instanceId = 0L;
        }
        List<DeployAppEnvAutoConfigKeyValueVo> keyValueList = new ArrayList<>();
        JSONArray keyValueArray = paramObj.getJSONArray("keyValueList");
        if (CollectionUtils.isNotEmpty(keyValueArray)) {
            keyValueList = keyValueArray.toJavaList(DeployAppEnvAutoConfigKeyValueVo.class);
        }
        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(appSystemId, paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(appSystemId, DeployAppConfigAction.EDIT);
        Set<String> keySet = new HashSet<>();
        for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : keyValueList ) {
            if (Objects.equals(keyValueVo.getIsEmpty(), 1)) {
                keyValueVo.setValue(StringUtils.EMPTY);
            } else {
                if (StringUtils.isBlank(keyValueVo.getValue())) {
                    keyValueVo.setValue(null);
                }
            }
            if (keySet.contains(keyValueVo.getKey())) {
                throw new DeployAppConfigEnvAutoConfigKeyRepeatException(keyValueVo.getKey());
            }
            keySet.add(keyValueVo.getKey());
        }
        if (instanceId != 0L) {
            DeployAppEnvAutoConfigVo appEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, 0L);
            List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(appEnvAutoConfigVo);
            Map<String, DeployAppEnvAutoConfigKeyValueVo> oldKeyValueMap = oldKeyValueList.stream().filter(Objects::nonNull).collect(Collectors.toMap(DeployAppEnvAutoConfigKeyValueVo::getKey, e  -> e));
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : keyValueList) {
                DeployAppEnvAutoConfigKeyValueVo oldKeyValueVo = oldKeyValueMap.get(keyValueVo.getKey());
                if (oldKeyValueVo == null) {
                    throw new DeployAppConfigEnvAutoConfigKeyIrregularException(keyValueVo.getKey());
                }
                if (!Objects.equals(oldKeyValueVo.getType(), keyValueVo.getType())) {
                    throw new DeployAppConfigEnvAutoConfigKeyTypeIrregularException(keyValueVo.getKey(), keyValueVo.getType(), oldKeyValueVo.getType());
                }
            }
        }
//        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSON.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, instanceId);
        List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList = deployAppConfigMapper.getAppEnvAutoConfigKeyValueList(appEnvAutoConfigVo);
        JSONArray tbodyList = getTbodyList(oldKeyValueList, keyValueList);
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            Date nowDate = new Date(System.currentTimeMillis());
            appEnvAutoConfigVo.setLcd(nowDate);
            if (CollectionUtils.isNotEmpty(keyValueList)) {
                appEnvAutoConfigVo.setKeyValueList(keyValueList);
                deployAppConfigMapper.insertAppEnvAutoConfig(appEnvAutoConfigVo);
            }
            deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
            DeployAppEnvAutoConfigAuditVo deployAppEnvAutoConfigAuditVo = new DeployAppEnvAutoConfigAuditVo();
            deployAppEnvAutoConfigAuditVo.setAppSystemId(appSystemId);
            deployAppEnvAutoConfigAuditVo.setAppModuleId(appModuleId);
            deployAppEnvAutoConfigAuditVo.setEnvId(envId);
            deployAppEnvAutoConfigAuditVo.setInstanceId(instanceId);
            deployAppEnvAutoConfigAuditVo.setConfig(TableResultUtil.getResult(tbodyList));
            deployAppConfigMapper.insertAppEnvAutoConfigAudit(deployAppEnvAutoConfigAuditVo);
        }
        Long deleteInstanceId = paramObj.getLong("deleteInstanceId");
        if (deleteInstanceId != null) {
            DeployAppEnvAutoConfigVo deleteAppEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(appSystemId, appModuleId, envId, deleteInstanceId);
            deployAppConfigMapper.deleteAppEnvAutoConfig(deleteAppEnvAutoConfigVo);
        }
        return null;
    }

    private JSONArray getTbodyList(List<DeployAppEnvAutoConfigKeyValueVo> oldKeyValueList, List<DeployAppEnvAutoConfigKeyValueVo> newKeyValueList) {
        oldKeyValueList.sort(Comparator.comparing(DeployAppEnvAutoConfigKeyValueVo::getKey));
        newKeyValueList.sort(Comparator.comparing(DeployAppEnvAutoConfigKeyValueVo::getKey));
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(oldKeyValueList)) {
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : oldKeyValueList) {
                JSONObject tbody = new JSONObject();
                tbody.put("key", keyValueVo.getKey());
                tbody.put("beforeType", keyValueVo.getType());
                tbody.put("beforeValue", keyValueVo.getValue());
                tbody.put("beforeIsEmpty", keyValueVo.getIsEmpty());
                tbody.put("action", "delete");
                tbodyList.add(tbody);
            }
        }
        if (CollectionUtils.isNotEmpty(newKeyValueList)) {
            int lastIndex = -1;
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : newKeyValueList) {
                Integer index = null;
                for (int i = 0; i < tbodyList.size(); i++) {
                    JSONObject tbody = tbodyList.getJSONObject(i);
                    String key = tbody.getString("key");
                    if (Objects.equals(key, keyValueVo.getKey())) {
                        index = i;
                    }
                }
                if (index != null) {
                    JSONObject tbody = tbodyList.getJSONObject(index);
                    tbody.put("afterType", keyValueVo.getType());
                    tbody.put("afterValue", keyValueVo.getValue());
                    tbody.put("afterIsEmpty", keyValueVo.getIsEmpty());
                    tbody.put("action", "update");
                    lastIndex = index;
                } else {
                    lastIndex++;
                    JSONObject tbody = new JSONObject();
                    tbody.put("key", keyValueVo.getKey());
                    tbody.put("afterType", keyValueVo.getType());
                    tbody.put("afterValue", keyValueVo.getValue());
                    tbody.put("afterIsEmpty", keyValueVo.getIsEmpty());
                    tbody.put("action", "insert");
                    tbodyList.add(lastIndex, tbody);
                }
            }
        }
        for (int index = tbodyList.size() - 1; index >= 0; index--) {
            JSONObject tbody = tbodyList.getJSONObject(index);
            String action = tbody.getString("action");
            if (Objects.equals(action, "update")) {
                Integer beforeIsEmpty = tbody.getInteger("beforeIsEmpty");
                Integer afterIsEmpty = tbody.getInteger("afterIsEmpty");
                if (Objects.equals(beforeIsEmpty, afterIsEmpty)) {
                    if (Objects.equals(beforeIsEmpty, 1)) {
                        tbodyList.remove(index);
                    } else {
                        String beforeValue = tbody.getString("beforeValue");
                        String afterValue = tbody.getString("afterValue");
                        if (Objects.equals(beforeValue, afterValue)) {
                            tbodyList.remove(index);
                        }
//                        else {
//                            if (StringUtils.isBlank(beforeValue) && StringUtils.isBlank(afterValue)) {
//                                tbodyList.remove(index);
//                            }
//                        }
                    }
                }
            }
        }
        return tbodyList;
    }
}
