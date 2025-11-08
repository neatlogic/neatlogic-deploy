/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.deploy.dependency.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.constvalue.FrameworkFromType;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/12/29 16:36
 */
@Service
public class NotifyPolicyDeployJobDependencyHandler extends DefaultDependencyHandlerBase {

//    @Resource
//    DeployAppConfigMapper deployAppConfigMapper;
//
//    @Override
//    protected String getTableName() {
//        return "deploy_job_notify_policy";
//    }
//
//    @Override
//    protected String getFromField() {
//        return "notify_policy_id";
//    }
//
//    @Override
//    protected String getToField() {
//        return "app_system_id";
//    }
//
//    @Override
//    protected List<String> getToFieldList() {
//        return Arrays.asList("app_system_id", "config");
//    }
//
//    @Override
//    protected DependencyInfoVo parse(Object dependencyObj) {
//        if (dependencyObj instanceof Map) {
//            Map<String, Object> map = (Map) dependencyObj;
//            Long appSystemId = (Long) map.get("app_system_id");
//            DeployAppConfigVo appConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId));
//            if (appConfigVo != null) {
//                IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
//                AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
//                if (appSystemVo != null) {
//                    String lastName = appSystemVo.getAbbrName() + (StringUtils.isNotEmpty(appSystemVo.getName()) ? "(" + appSystemVo.getName() + ")" : "");
//                    JSONObject dependencyInfoConfig = new JSONObject();
//                    dependencyInfoConfig.put("appSystemId", appConfigVo.getAppSystemId());
//                    List<String> pathList = new ArrayList<>();
//                    pathList.add("应用配置");
//                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/deploy.html#/application-config-manage?appSystemId=${DATA.appSystemId}";
//                    return new DependencyInfoVo(appConfigVo.getAppSystemId(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
//                }
//            }
//        }
//        return null;
//    }

    @Override
    public IFromType getFromType() {
        return FrameworkFromType.NOTIFY_POLICY;
    }

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        Long appSystemId = Long.valueOf(dependencyVo.getTo());
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
        if (appSystemVo != null) {
            String lastName = appSystemVo.getAbbrName() + (StringUtils.isNotEmpty(appSystemVo.getName()) ? "(" + appSystemVo.getName() + ")" : "");
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("appSystemId", appSystemId);
            List<String> pathList = new ArrayList<>();
            pathList.add("应用配置");
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/deploy.html#/application-config-manage?appSystemId=${DATA.appSystemId}";
            return new DependencyInfoVo(appSystemId, dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
        }
        return null;
    }
}
