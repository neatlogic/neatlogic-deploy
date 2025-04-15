ALTER TABLE `deploy_app_env_auto_config`
    ADD COLUMN `is_empty` TINYINT (1) DEFAULT 0 NOT NULL COMMENT '是否设为空' AFTER `value`;

