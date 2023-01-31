/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.dependency.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.CustomTableDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.deploy.dto.app.DeployAppConfigVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/12/29 16:36
 */
@Service
public class NotifyPolicyDeployJobDependencyHandler extends CustomTableDependencyHandlerBase {

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
        return null;
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
