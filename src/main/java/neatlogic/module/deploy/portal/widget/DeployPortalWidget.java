/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.deploy.portal.widget;

import neatlogic.framework.portal.widget.core.IPortalWidget;
import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum DeployPortalWidget implements IPortalWidget {
    deployPendingRelease("deployPendingRelease", "待发布版本", 1, DeployPortalWidgetGroup.deployGroup1),
    deployRunningDeployment("deployRunningDeployment", "执行中部署", 2, DeployPortalWidgetGroup.deployGroup1),
    deployReleaseRisk("deployReleaseRisk", "失败与风险发布", 3, DeployPortalWidgetGroup.deployGroup2),
    deployReleaseCalendar("deployReleaseCalendar", "发布日程与维护窗口", 4, DeployPortalWidgetGroup.deployGroup2),
    ;
    private final String value;
    private final String text;
    private final Integer sort;
    private final IPortalWidgetGroup group;

    DeployPortalWidget(String value, String text, Integer sort, IPortalWidgetGroup group) {
        this.value = value;
        this.text = text;
        this.sort = sort;
        this.group = group;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public Integer getSort() {
        return this.sort;
    }

    @Override
    public IPortalWidgetGroup getGroup() {
        return this.group;
    }
}
