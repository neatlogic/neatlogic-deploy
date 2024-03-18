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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class updateDeployAppPipelineParamEditableFieldApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/param/editable/update";
    }

    @Override
    public String getName() {
        return "更新应用流水线作业参数的可编辑字段值";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Description(desc = "更新应用流水线作业参数的可编辑字段值")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray resultList = new JSONArray();
        int rowNum = deployAppConfigMapper.getAllDeployAppConfigCount();
        if (rowNum > 0) {
            BasePageVo searchVo = new BasePageVo();
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<DeployAppConfigVo> deployAppConfigList = deployAppConfigMapper.getAllAppConfigListByPage(searchVo);
                for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
                    DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
                    if (config != null) {
                        List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
                        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                            for (AutoexecParamVo paramVo : runtimeParamList) {
                                paramVo.setEditable(1);
                            }
                            config.setRuntimeParamList(runtimeParamList);
                            deployAppConfigVo.setConfig(config);
                            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
                            JSONObject obj = new JSONObject();
                            obj.put("appSystemId", deployAppConfigVo.getAppSystemId());
                            obj.put("appModuleId", deployAppConfigVo.getAppModuleId());
                            obj.put("envId", deployAppConfigVo.getEnvId());
                            resultList.add(obj);
                        }
                    }
                }
            }
        }
        return resultList;
    }

}
