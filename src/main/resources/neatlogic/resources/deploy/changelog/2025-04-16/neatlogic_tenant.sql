ALTER TABLE `deploy_app_env_auto_config`
    CHANGE `type` `type` ENUM ('text', 'password', 'textarea') CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'text' NULL COMMENT '变量类型';

