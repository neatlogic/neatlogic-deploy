CREATE TABLE IF NOT EXISTS `deploy_blue_green`  (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '姓名',
  `sort` int DEFAULT NULL COMMENT '排序',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布蓝绿表';

CREATE TABLE IF NOT EXISTS `deploy_app_instance_blue_green`  (
  `app_system_id` bigint NOT NULL COMMENT '系统id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `resource_id` bigint NOT NULL COMMENT '实例id',
  `blue_green_id` bigint DEFAULT NULL COMMENT '蓝绿id',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布实例蓝绿关系表';



CREATE TABLE IF NOT EXISTS `deploy_job_phase_node_blue_green`  (
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `job_phase_id` bigint NOT NULL COMMENT '作业阶段id',
  `job_phase_node_id` bigint NOT NULL COMMENT '作业节点ID',
  `blue_green_id` bigint NOT NULL COMMENT '蓝绿ID',
  `update_tag` bigint DEFAULT NULL COMMENT '跟新标记',
  PRIMARY KEY (`job_phase_node_id`,`blue_green_id`),
  KEY `idx_phase_id` (`job_phase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业蓝绿id';