ALTER TABLE `deploy_app_env_auto_config` ADD COLUMN `type` enum('text','password') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'text' COMMENT '变量类型' AFTER `key`;
