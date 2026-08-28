/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.deploy.service;

import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import net.sf.jsqlparser.schema.Column;

import java.util.List;
import java.util.Map;

public interface DeployResourceBuildSqlService {

    String buildGetDatabaseByIdSql(Long id);

    String buildGetCmdbDeployAppEnvListByAppSystemIdAndModuleIdListSql(Long appSystemId, List<Long> appModuleIdList);

    String buildGetCmdbHasEnvAppModuleIdListByAppSystemIdAndModuleIdListSql(Long appSystemId, List<Long> appModuleIdList);

    String buildGetCmdbEnvListByAppSystemIdAndModuleIdSql(Long appSystemId, Long appModuleId);

    String buildGetCmdbDeployAppModuleEnvListByAppSystemIdSql(Long appSystemId);

    String buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(Long appSystemId, List<Long> appModuleIdList);

    String buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndModuleIdSql(Long systemId, Long moduleId);

    String buildGetCmdbAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvIdSql(Long appSystemId, Long appModuleId, List<Long> envIdList);

    String buildGetAppModuleEnvAutoConfigInstanceIdCountSql(DeployAppEnvAutoConfigVo searchVo);

    /** 同次构建返回真实列映射，供联合条件探针使用。 */
    String buildGetAppModuleEnvAutoConfigInstanceIdCountSql(DeployAppEnvAutoConfigVo searchVo, Map<String, Column> fieldName2ColumnMap);

    String buildGetAppConfigEnvDatabaseCountSql(DeployResourceSearchVo searchVo);

    String buildGetAppConfigEnvDatabaseResourceIdListSql(DeployResourceSearchVo searchVo);
}
