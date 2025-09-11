ALTER TABLE `deploy_job`
MODIFY COLUMN `app_system_id` bigint NULL COMMENT '系统id' AFTER `id`;