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

import neatlogic.framework.cmdb.crossover.IResourceBuildSqlCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.config.*;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import neatlogic.framework.sqlgenerator.$sql;
import neatlogic.framework.sqlgenerator.ExpressionVo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeployResourceBuildSqlServiceImpl implements DeployResourceBuildSqlService {
    private final Logger logger = LoggerFactory.getLogger(DeployResourceBuildSqlServiceImpl.class);

    @Override
    public String buildGetDatabaseByIdSql(Long id) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_database_ip_port_env_appmodule");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("type_name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addWhereExpression(plainSelect, $sql.exp(idColumn.toString(), "=", id));
            $sql.setLimit(plainSelect, 1);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetCmdbDeployAppEnvListByAppSystemIdAndModuleIdListSql(Long appSystemId, List<Long> appModuleIdList) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_appinstance_env_appmodule_appsystem");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("env_name");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("app_module_name");
            filterItemFieldNameList.add("app_system_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_id").toString(), "envId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_name").toString(), "envName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_id").toString(), "appModuleId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_name").toString(), "appModuleName");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is not null"));
            if (appSystemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", appSystemId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "is not null"));
            }
            if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", appModuleIdList));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "is not null"));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetCmdbDeployAppModuleEnvListByAppSystemIdSql(Long appSystemId) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_appinstance_env_appmodule_appsystem");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("env_name");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("app_system_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_id").toString(), "envId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_name").toString(), "envName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_id").toString(), "appModuleId");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is not null"));
            if (appSystemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", appSystemId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(Long appSystemId, List<Long> appModuleIdList) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_appinstance_env_appmodule_appsystem");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("app_system_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_id").toString(), "envId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_id").toString(), "appModuleId");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is not null"));
            if (appSystemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", appSystemId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "is not null"));
//                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", appModuleIdList));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "is not null"));
//                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndModuleIdSql(Long systemId, Long moduleId) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_appinstance_env_appmodule_appsystem");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("env_name");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_id").toString(), "envId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_name").toString(), "envName");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is not null"));
            if (systemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", systemId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "is not null"));
            }
            if (moduleId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", moduleId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "is not null"));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetCmdbAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvIdSql(Long appSystemId, Long appModuleId, List<Long> envIdList) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_appinstance_env_appmodule_appsystem");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            String appSystemIdColumn = fieldName2ColumnMap.get("app_system_id").toString();
            String appModuleIdColumn = fieldName2ColumnMap.get("app_module_id").toString();
            String envIdColumn = fieldName2ColumnMap.get("env_id").toString();
            $sql.addSelectColumn(plainSelect, appSystemIdColumn, "appSystemId");
            $sql.addSelectColumn(plainSelect, appModuleIdColumn, "appModuleId");
            $sql.addSelectColumn(plainSelect, envIdColumn, "envId");
            $sql.addSelectColumn(plainSelect, "daced.`db_schema`", "`dbSchema`");
            $sql.addSelectColumn(plainSelect, "daced.`config`", "`configStr`");
            $sql.addSelectColumn(plainSelect, "daeac.`key`", "`key`");
            $sql.addSelectColumn(plainSelect, "daeac.`value`", "`value`");
            $sql.addSelectColumn(plainSelect, "daeac.`type`", "`type`");
            $sql.addSelectColumn(plainSelect, "daeac.`is_empty`", "`isEmpty`");
            ExpressionVo dacedOn = $sql.exp(
                    $sql.exp(appSystemIdColumn, "=", "daced.app_system_id"),
                    "AND",
                    $sql.exp($sql.exp(appModuleIdColumn, "=", "daced.app_module_id"), "AND", $sql.exp(envIdColumn, "=", "daced.env_id"))
            );
            $sql.addJoin(plainSelect, "left join", "deploy_app_config_env_db", "daced", dacedOn);
            ExpressionVo daeacOn = $sql.exp(
                    $sql.exp(appSystemIdColumn, "=", "daeac.app_system_id"),
                    "AND",
                    $sql.exp(
                            $sql.exp(appModuleIdColumn, "=", "daeac.app_module_id"),
                            "AND",
                            $sql.exp($sql.exp(envIdColumn, "=", "daeac.env_id"), "AND", $sql.exp("daeac.instance_id", "=", 0))
                    )
            );
            $sql.addJoin(plainSelect, "left join", "deploy_app_env_auto_config", "daeac", daeacOn);
            if (appSystemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(appSystemIdColumn, "=", appSystemId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(appSystemIdColumn, "is not null"));
            }
            if (appModuleId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(appModuleIdColumn, "=", appModuleId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(appModuleIdColumn, "is not null"));
            }
            if (CollectionUtils.isNotEmpty(envIdList)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(envIdColumn, "in", envIdList));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(envIdColumn, "is not null"));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppConfigEnvDatabaseCountSql(DeployResourceSearchVo searchVo) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_database_ip_port_env_appmodule");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            if (CollectionUtils.isNotEmpty(searchVo.getDefaultValue())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("id").toString(), "in", searchVo.getDefaultValue()));
            }
            String keyword = searchVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                keyword = "%" + keyword + "%";
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                Column portColumn = fieldName2ColumnMap.get("port");
                ExpressionVo orExp = $sql.exp(nameColumn.toString(), "like", $sql.value(keyword));
                orExp = $sql.exp(orExp, "OR", $sql.exp(ipColumn.toString(), "like", $sql.value(keyword)));
                orExp = $sql.exp(orExp, "OR", $sql.exp(portColumn.toString(), "like", $sql.value(keyword)));
                $sql.addWhereExpression(plainSelect,
                        $sql.exp("(", orExp, ")"));
            }
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, $sql.fun("COUNT", idColumn.toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    @Override
    public String buildGetAppConfigEnvDatabaseResourceIdListSql(DeployResourceSearchVo searchVo) {
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_database_ip_port_env_appmodule");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            if (CollectionUtils.isNotEmpty(searchVo.getDefaultValue())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("id").toString(), "in", searchVo.getDefaultValue()));
            }
            String keyword = searchVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                keyword = "%" + keyword + "%";
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                Column portColumn = fieldName2ColumnMap.get("port");
                ExpressionVo orExp = $sql.exp(nameColumn.toString(), "like", $sql.value(keyword));
                orExp = $sql.exp(orExp, "OR", $sql.exp(ipColumn.toString(), "like", $sql.value(keyword)));
                orExp = $sql.exp(orExp, "OR", $sql.exp(portColumn.toString(), "like", $sql.value(keyword)));
                $sql.addWhereExpression(plainSelect, $sql.exp("(", orExp, ")"));
            }
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, idColumn.toString());
            $sql.setDistinct(plainSelect, true);
            if (searchVo.getNeedPage()) {
                $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
