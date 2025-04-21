CREATE TABLE IF NOT EXISTS `deploy_blue_green`  (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) NULL COMMENT '姓名',
  `sort` integer NULL COMMENT '排序',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否激活',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uniqu`(`id`)
) COMMENT = '发布蓝绿表';

CREATE TABLE IF NOT EXISTS `deploy_app_instance_blue_green`  (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NULL COMMENT '系统id',
  `app_module_id` bigint NULL COMMENT '模块id',
  `env_id` bigint NULL COMMENT '环境id',
  `resource_id` bigint NULL COMMENT '实例id',
  `blue_green_id` bigint NULL COMMENT '蓝绿id',
   PRIMARY KEY (`id`, `app_system_id`, `app_module_id`, `env_id`, `resource_id`) USING BTREE
) COMMENT = '发布实例蓝绿关系表';


CREATE TABLE IF NOT EXISTS `deploy_job_phase_node_blue_green`  (
  `job_phase_node_id` bigint NOT NULL COMMENT '作业节点ID',
  `blue_green_id` bigint NOT NULL COMMENT '蓝绿ID',
  PRIMARY KEY (`job_phase_node_id`, `blue_green_id`)
) COMMENT = '发布作业蓝绿id';