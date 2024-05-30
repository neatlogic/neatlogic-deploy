CREATE TABLE `deploy_app_config_env_attr` (
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境资产id',
  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变量名',
  `type` enum('text','password') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'text' COMMENT '变量类型',
  `value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变量值',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一次修改人',
  `update_time` bigint DEFAULT NULL COMMENT '最后一次修改时间',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用环境属性';