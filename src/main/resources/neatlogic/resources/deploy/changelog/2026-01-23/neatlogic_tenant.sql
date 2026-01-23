ALTER TABLE `deploy_pipeline`
ADD COLUMN `default_version` varchar(255) NULL COMMENT '默认版本' AFTER `app_system_id`;