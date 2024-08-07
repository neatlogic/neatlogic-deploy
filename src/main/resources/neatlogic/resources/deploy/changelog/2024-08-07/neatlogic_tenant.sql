ALTER TABLE `deploy_version`
    MODIFY COLUMN `repo` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '仓库地址' AFTER `repo_type`,
    MODIFY COLUMN `trunk` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主干' AFTER `repo`,
    MODIFY COLUMN `branch` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分支' AFTER `trunk`,
    MODIFY COLUMN `tag` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签' AFTER `branch`;

ALTER TABLE `deploy_sql_detail`
    MODIFY COLUMN `sql_file` VARCHAR(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件名称';
