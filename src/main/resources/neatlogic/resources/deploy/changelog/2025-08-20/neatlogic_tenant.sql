ALTER TABLE `deploy_job`
MODIFY COLUMN `app_module_id` bigint NULL COMMENT '系统模块id' AFTER `app_system_id`;

ALTER TABLE `deploy_job`
MODIFY COLUMN `env_id` bigint NULL COMMENT '环境id' AFTER `app_module_id`;