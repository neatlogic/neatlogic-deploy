CREATE TABLE IF NOT EXISTS `deploy_app_env_auto_config_audit` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境资产id',
  `instance_id` bigint NOT NULL COMMENT '实例资产id',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改记录',
  `fcd` timestamp(3) NOT NULL COMMENT '创建者',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用环境变量配置审计';

