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

@ResourceType(name = "scence_database_ip_port_env_appmodule", label = "DB库IP端口环境和模块场景", moduleId= "deploy", functionPathList = {"查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境的数据库接口/deploy/app/config/env/database/search"})
public class DataBaseIpPortEnvAppModuleVo {
    @EntityField(name = "ID", type = ApiParamType.LONG)
    @ResourceField(name = "id")
    private Long id;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.name.name", type = ApiParamType.STRING)
    @ResourceField(name = "name")
    private String name;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.ip.name", type = ApiParamType.STRING)
    @ResourceField(name = "ip")
    private String ip;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.port.name", type = ApiParamType.INTEGER)
    @ResourceField(name = "port")
    private Integer port;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.typeid.name", type = ApiParamType.LONG)
    @ResourceField(name = "type_id")
    private Long typeId;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.typename.name", type = ApiParamType.STRING)
    @ResourceField(name = "type_name")
    private String typeName;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.typelabel.name", type = ApiParamType.STRING)
    @ResourceField(name = "type_label")
    private String typeLabel;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.envid.name", type = ApiParamType.LONG)
    @ResourceField(name = "env_id")
    private Long envId;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.envname.name", type = ApiParamType.STRING)
    @ResourceField(name = "env_name")
    private String envName;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.envseqno.name", type = ApiParamType.INTEGER)
    @ResourceField(name = "env_seq_no")
    private Integer envSeqNo;

    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.appmoduleid.name", type = ApiParamType.LONG)
    @ResourceField(name = "app_module_id")
    private Long appModuleId;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.appmodulename.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_module_name")
    private String appModuleName;
    @EntityField(name = "nmddrs.databaseipportenvappmodulevo.appmoduleabbrname.name", type = ApiParamType.STRING)
    @ResourceField(name = "app_module_abbr_name")
    private String appModuleAbbrName;
}
