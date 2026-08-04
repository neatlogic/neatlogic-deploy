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

package neatlogic.module.deploy.dto.resourcecenter.sceneviewfielddeclare;

import neatlogic.framework.cmdb.annotation.ResourceField;
import neatlogic.framework.cmdb.annotation.ResourceType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.EntityField;

@ResourceType(name = "scence_appinstance_env_appmodule_appsystem", label = "应用实例环境和模块及应用场景", moduleId= "deploy", functionPathList = {"集成与发布模块"})
public class AppInstanceEnvAppModuleAppSystemVo {
    @EntityField(name = "ID", type = ApiParamType.LONG)
    @ResourceField(name = "id")
    private Long id;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.name.name", type = ApiParamType.STRING)
    @ResourceField(name = "name")
    private String name;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.ip.name", type = ApiParamType.STRING)
    @ResourceField(name = "ip")
    private String ip;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.port.name", type = ApiParamType.INTEGER)
    @ResourceField(name = "port")
    private Integer port;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.typeid.name", type = ApiParamType.LONG)
    @ResourceField(name = "type_id")
    private Long typeId;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.typename.name", type = ApiParamType.STRING)
    @ResourceField(name = "type_name")
    private String typeName;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.typelabel.name", type = ApiParamType.STRING)
    @ResourceField(name = "type_label")
    private String typeLabel;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.envid.name", type = ApiParamType.LONG)
    @ResourceField(name = "env_id")
    private Long envId;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.envname.name", type = ApiParamType.STRING)
    @ResourceField(name = "env_name")
    private String envName;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.envseqno.name", type = ApiParamType.INTEGER)
    @ResourceField(name = "env_seq_no")
    private Integer envSeqNo;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appmoduleid.name", type = ApiParamType.LONG)
    @ResourceField(name = "app_module_id")
    private Long appModuleId;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appmodulename.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_module_name")
    private String appModuleName;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appmoduleabbrname.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_module_abbr_name")
    private String appModuleAbbrName;

    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appsystemid.name", type = ApiParamType.LONG)
    @ResourceField(name = "app_system_id")
    private Long appSystemId;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appsystemname.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_system_name")
    private String appSystemName;
    @EntityField(name = "nmddrs.appinstanceenvappmoduleappsystemvo.appsystemabbrname.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_system_abbr_name")
    private String appSystemAbbrName;
}
