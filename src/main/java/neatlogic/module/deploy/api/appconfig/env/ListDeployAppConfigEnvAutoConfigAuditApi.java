/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigAuditVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ListDeployAppConfigEnvAutoConfigAuditApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getName() {
        return "查询应用环境实例autoConfig审计列表";
    }
    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "应用实例 id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, defaultValue = "1", desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, defaultValue = "10",  desc = "common.pagesize"),
    })
    @Output({
    })
    @Description(desc = "查询应用环境实例autoConfig审计列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        JSONArray tableList = new JSONArray();
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        Long envId = paramObj.getLong("envId");
        Long instanceId = paramObj.getLong("instanceId");
        if (instanceId == null) {
            instanceId = 0L;
        }
        Integer currentPage = paramObj.getInteger("currentPage");
        Integer pageSize = paramObj.getInteger("pageSize");
        DeployAppEnvAutoConfigAuditVo deployAppEnvAutoConfigAuditVo = new DeployAppEnvAutoConfigAuditVo();
        deployAppEnvAutoConfigAuditVo.setAppSystemId(appSystemId);
        deployAppEnvAutoConfigAuditVo.setAppModuleId(appModuleId);
        deployAppEnvAutoConfigAuditVo.setEnvId(envId);
        deployAppEnvAutoConfigAuditVo.setInstanceId(instanceId);
        deployAppEnvAutoConfigAuditVo.setCurrentPage(currentPage);
        deployAppEnvAutoConfigAuditVo.setPageSize(pageSize);
        int rowNum = deployAppConfigMapper.getAppEnvAutoConfigAuditCount(deployAppEnvAutoConfigAuditVo);
        if (rowNum > 0) {
            deployAppEnvAutoConfigAuditVo.setRowNum(rowNum);
            Map<String, UserVo> userMap = new HashMap<>();
            List<DeployAppEnvAutoConfigAuditVo> list = deployAppConfigMapper.getAppEnvAutoConfigAuditList(deployAppEnvAutoConfigAuditVo);
            Set<String> userUuidSet = list.stream().filter(Objects::nonNull).map(DeployAppEnvAutoConfigAuditVo::getFcu).filter(Objects::nonNull).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(userUuidSet)) {
                List<UserVo> userList = userMapper.getUserByUserUuidList(new ArrayList<>(userUuidSet));
                if (CollectionUtils.isNotEmpty(userList)) {
                    userMap = userList.stream().collect(Collectors.toMap(UserVo::getUuid, e -> e));
                }
            }
            for (DeployAppEnvAutoConfigAuditVo appEnvAutoConfigAuditVo : list) {
                JSONObject tableObj = new JSONObject();
                Long id = appEnvAutoConfigAuditVo.getId();
                String fcu = appEnvAutoConfigAuditVo.getFcu();
                Date fcd = appEnvAutoConfigAuditVo.getFcd();
                JSONObject config = appEnvAutoConfigAuditVo.getConfig();
                JSONArray tbodyList = config.getJSONArray("tbodyList");
                for (int i = 0; i < tbodyList.size(); i++) {
                    JSONObject tbody = tbodyList.getJSONObject(i);
                    String key = tbody.getString("key");
                    tbody.put("uuid", id + "_" + key);
//                    String action = tbody.getString("action");
//                    if (Objects.equals(action, "insert")) {
//                        tbody.put("colorClass", "bg-success-grey");
//                    } else if (Objects.equals(action, "update")) {
//                        tbody.put("colorClass", "bg-warning-grey");
//                    } else if (Objects.equals(action, "delete")) {
//                        tbody.put("colorClass", "bg-error-grey");
//                    }
                    Integer beforeIsEmpty = tbody.getInteger("beforeIsEmpty");
                    if (Objects.equals(beforeIsEmpty, 1)) {
                        tbody.put("beforeValue", "设为空");
                    }
                    Integer afterIsEmpty = tbody.getInteger("afterIsEmpty");
                    if (Objects.equals(afterIsEmpty, 1)) {
                        tbody.put("afterValue", "设为空");
                    }
                    String beforeType = tbody.getString("beforeType");
                    if (StringUtils.isNotBlank(beforeType)) {
                        ParamType paramType = ParamType.getParamType(beforeType);
                        if (paramType != null) {
                            tbody.put("beforeTypeText", paramType.getText());
                        }
                    }
                    String afterType = tbody.getString("afterType");
                    if (StringUtils.isNotBlank(afterType)) {
                        ParamType paramType = ParamType.getParamType(afterType);
                        if (paramType != null) {
                            tbody.put("afterTypeText", paramType.getText());
                        }
                    }
                }
                tableObj.put("id", id);
                tableObj.put("fcd", fcd);
                tableObj.put("fcu", fcu);
                tableObj.put("_expand", true);
                UserVo userVo = userMap.get(fcu);
                if (userVo != null) {
                    tableObj.put("fcuName", userVo.getUserName());
                } else {
                    tableObj.put("fcuName", "");
                }
                JSONObject table = new JSONObject();
                table.put("tbodyList", tbodyList);
                tableObj.put("table", table);
                tableList.add(tableObj);
            }
        }
        resultObj.put("tableList", tableList);
        return TableResultUtil.getResult(tableList, deployAppEnvAutoConfigAuditVo);
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/audit/list";
    }
}
