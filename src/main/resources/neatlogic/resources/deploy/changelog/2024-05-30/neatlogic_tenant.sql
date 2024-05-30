ALTER TABLE `deploy_app_config_env_attr`
MODIFY COLUMN `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'text' COMMENT '变量类型' AFTER `key`;