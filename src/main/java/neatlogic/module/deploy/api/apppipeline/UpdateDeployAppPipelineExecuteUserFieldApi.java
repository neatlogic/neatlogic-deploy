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

package neatlogic.module.deploy.api.apppipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployAppPipelineExecuteUserFieldApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/executeuser/update";
    }

    @Override
    public String getName() {
        return "更新应用流水线的执行用户字段值";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Description(desc = "更新应用流水线的执行用户字段值")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        int rowNum = deployAppConfigMapper.getAllDeployAppConfigCount();
        resultObj.put("总数", rowNum);
        if (rowNum == 0) {
            return resultObj;
        }
        int needUpdateCount = 0;
        int updatedCount = 0;
        BasePageVo searchVo = new BasePageVo();
        searchVo.setRowNum(rowNum);
        searchVo.setPageSize(100);
        int pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<Map<String, Object>> list = deployAppConfigMapper.getDeployAppConfigListForUpdateConfig(searchVo);
            for (Map<String, Object> version : list) {
                Long id = (Long) version.get("id");
                String configStr = (String) version.get("config");
                if (StringUtils.isBlank(configStr)) {
                    continue;
                }
                JSONObject config = null;
                try {
                    config = JSONObject.parseObject(configStr);
                } catch (JSONException e) {
                    System.out.println("格式不对");
                }
                if (MapUtils.isEmpty(config)) {
                    continue;
                }
                try {
                    DeployPipelineConfigVo configVo = config.toJavaObject(DeployPipelineConfigVo.class);
                    continue;
                } catch (JSONException e) {
                }
                needUpdateCount++;
//                if (Objects.equals(id, 818075860795538L)) {
//                    System.out.println("");
//                }
                boolean flag = false;
                {
                    JSONObject executeConfig = config.getJSONObject("executeConfig");
                    if (updateExecuteConfig(executeConfig)) {
                        flag = true;
                    }
                }
                JSONArray combopGroupList = config.getJSONArray("combopGroupList");
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (int i = 0; i < combopGroupList.size(); i++) {
                        JSONObject group = combopGroupList.getJSONObject(i);
                        JSONObject groupConfig = group.getJSONObject("config");
                        if (MapUtils.isEmpty(groupConfig)) {
                            continue;
                        }
                        JSONObject executeConfig = groupConfig.getJSONObject("executeConfig");
                        if (updateExecuteConfig(executeConfig)) {
                            flag = true;
                        }
                    }
                }
                JSONArray combopPhaseList = config.getJSONArray("combopPhaseList");
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (int i = 0; i < combopPhaseList.size(); i++) {
                        JSONObject phase = combopPhaseList.getJSONObject(i);
                        JSONObject phaseConfig = phase.getJSONObject("config");
                        if (MapUtils.isEmpty(phaseConfig)) {
                            continue;
                        }
                        JSONObject executeConfig = phaseConfig.getJSONObject("executeConfig");
                        if (updateExecuteConfig(executeConfig)) {
                            flag = true;
                        }
                    }
                }
                if (flag) {
                    try {
                        DeployPipelineConfigVo configVo = config.toJavaObject(DeployPipelineConfigVo.class);
                        deployAppConfigMapper.updateDeployAppConfigById(id, config.toJSONString());
                        updatedCount++;
                    } catch (JSONException e) {
                    }
                }
            }
        }
        resultObj.put("已更新个数", updatedCount);
        resultObj.put("需要更新个数", needUpdateCount);
        return resultObj;
    }

    /**
     * 更新执行目标配置executeConfig
     * @param executeConfig
     * @return 返回标识更新前后是否发生变化
     */
    private boolean updateExecuteConfig(JSONObject executeConfig) {
        if (MapUtils.isNotEmpty(executeConfig)) {
            Object executeUser = executeConfig.get("executeUser");
            if (executeUser == null) {
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                paramMappingVo.setValue("");
                executeConfig.put("executeUser", paramMappingVo);
                return true;
            } else {
                if (executeUser instanceof String) {
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    paramMappingVo.setValue(executeUser);
                    executeConfig.put("executeUser", paramMappingVo);
                    return true;
                }
            }
        }
        return false;
    }

}
