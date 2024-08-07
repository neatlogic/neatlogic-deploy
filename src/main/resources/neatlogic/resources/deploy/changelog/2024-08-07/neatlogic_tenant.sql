ALTER TABLE `deploy_sql_detail`
    CHANGE `sql_file` `sql_file` VARCHAR(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件名称';
