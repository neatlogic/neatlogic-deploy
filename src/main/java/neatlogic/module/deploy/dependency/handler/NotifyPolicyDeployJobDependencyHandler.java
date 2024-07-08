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
package neatlogic.module.deploy.dependency.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.CustomDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/12/29 16:36
 */
@Service
public class NotifyPolicyDeployJobDependencyHandler extends CustomDependencyHandlerBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    protected String getTableName() {
        return "deploy_job_notify_policy";
    }

    @Override
    protected String getFromField() {
        return "notify_policy_id";
    }

    @Override
    protected String getToField() {
        return "app_system_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return Arrays.asList("app_system_id", "config");
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            Long appSystemId = (Long) map.get("app_system_id");
            DeployAppConfigVo appConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId));
            if (appConfigVo != null) {
                IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
                AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
                if (appSystemVo != null) {
                    String lastName = appSystemVo.getAbbrName() + (StringUtils.isNotEmpty(appSystemVo.getName()) ? "(" + appSystemVo.getName() + ")" : "");
                    JSONObject dependencyInfoConfig = new JSONObject();
                    dependencyInfoConfig.put("appSystemId", appConfigVo.getAppSystemId());
                    List<String> pathList = new ArrayList<>();
                    pathList.add("应用配置");
                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/deploy.html#/application-config-manage?appSystemId=${DATA.appSystemId}";
                    return new DependencyInfoVo(appConfigVo.getAppSystemId(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
                }
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.NOTIFY_POLICY;
    }
}
