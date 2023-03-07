-- ----------------------------
-- Table structure for deploy_app_config
-- ----------------------------
CREATE TABLE `deploy_app_config` (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `app_module_id` bigint NOT NULL DEFAULT '0' COMMENT '模块id',
  `env_id` bigint NOT NULL DEFAULT '0' COMMENT '环境id',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置信息',
  `fcd` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改用户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_appId_moduleId_envId` (`app_system_id`,`app_module_id`,`env_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用系统配置';

-- ----------------------------
-- Table structure for deploy_app_config_authority
-- ----------------------------
CREATE TABLE `deploy_app_config_authority` (
  `app_system_id` bigint NOT NULL COMMENT '应用资产id',
  `auth_type` enum('team','user','role','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权类型 user|team|role',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权用户对象',
  `action_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权操作类型',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权操作',
  `lcd` timestamp(3) NOT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '最后一次修改人',
  PRIMARY KEY (`app_system_id`,`auth_type`,`auth_uuid`,`action_type`,`action`) USING BTREE,
  KEY `idx_app_env_authuuid_action` (`app_system_id`,`action_type`,`auth_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用系统配置授权';

-- ----------------------------
-- Table structure for deploy_app_config_draft
-- ----------------------------
CREATE TABLE `deploy_app_config_draft` (
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL DEFAULT '0' COMMENT '环境id',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '流水线部分配置信息',
  `fcd` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改用户',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用系统模块环境流水线阶段关系表';

-- ----------------------------
-- Table structure for deploy_app_config_env
-- ----------------------------
CREATE TABLE `deploy_app_config_env` (
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `app_module_id` bigint NOT NULL COMMENT '应用模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用配置环境表';

-- ----------------------------
-- Table structure for deploy_app_config_env_db
-- ----------------------------
CREATE TABLE `deploy_app_config_env_db` (
  `id` bigint NOT NULL COMMENT 'id',
  `db_schema` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据库schema',
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `app_module_id` bigint NOT NULL COMMENT '应用模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `db_resource_id` bigint DEFAULT NULL COMMENT '数据库id',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '高级设置',
  `account_id` bigint DEFAULT NULL COMMENT '账号id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `un_app_system_id_app_module_id_env_id_db_schema` (`db_schema`,`app_system_id`,`app_module_id`,`env_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用配置环境层DB配置表';

-- ----------------------------
-- Table structure for deploy_app_config_env_db_account
-- ----------------------------
CREATE TABLE `deploy_app_config_env_db_account` (
  `id` bigint NOT NULL COMMENT 'id',
  `db_config_id` bigint NOT NULL COMMENT 'DB配置id',
  `account_alias` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户别名',
  `account_id` bigint NOT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用配置环境层DB配置账号表';

-- ----------------------------
-- Table structure for deploy_app_config_user
-- ----------------------------
CREATE TABLE `deploy_app_config_user` (
  `app_system_id` bigint NOT NULL COMMENT '应用资产id',
  `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收藏人',
  PRIMARY KEY (`app_system_id`,`user_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用系统配置收藏';

-- ----------------------------
-- Table structure for deploy_app_env_auto_config
-- ----------------------------
CREATE TABLE `deploy_app_env_auto_config` (
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境资产id',
  `instance_id` bigint NOT NULL COMMENT '实例资产id',
  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变量名',
  `value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变量值',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一次修改人',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`,`instance_id`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用环境变量配置';

-- ----------------------------
-- Table structure for deploy_app_module_runner_group
-- ----------------------------
CREATE TABLE `deploy_app_module_runner_group` (
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块资产id',
  `runner_group_id` bigint DEFAULT NULL COMMENT 'runner组id',
  PRIMARY KEY (`app_system_id`,`app_module_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布应用模块runner组关联';

-- ----------------------------
-- Table structure for deploy_ci
-- ----------------------------
CREATE TABLE `deploy_ci` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `is_active` tinyint(1) NOT NULL COMMENT '是否激活',
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `repo_type` enum('gitlab','svn') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '仓库类型',
  `repo_server_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '仓库服务器地址',
  `repo_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '仓库名',
  `branch_filter` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分支过滤',
  `event` enum('post-receive','post-commit') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '动作',
  `trigger_type` enum('auto','manual','instant','delay') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发类型',
  `trigger_time` char(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发时间',
  `delay_time` int DEFAULT NULL COMMENT '延迟时间',
  `version_rule` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版本号规则',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `hook_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'webhook id',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布持续集成表';

-- ----------------------------
-- Table structure for deploy_ci_audit
-- ----------------------------
CREATE TABLE `deploy_ci_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint NOT NULL COMMENT '持续集成配置id',
  `commit_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提交id',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `param_file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数内容文件路径',
  `result_file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结果内容文件路径',
  `error_file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误内容文件路径',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '触发时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布持续集成记录表';

-- ----------------------------
-- Table structure for deploy_env_version
-- ----------------------------
CREATE TABLE `deploy_env_version` (
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `build_no` int DEFAULT NULL COMMENT '编译号',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='环境当前版本';

-- ----------------------------
-- Table structure for deploy_env_version_audit
-- ----------------------------
CREATE TABLE `deploy_env_version_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `new_version_id` bigint NOT NULL COMMENT '新版本id',
  `old_version_id` bigint DEFAULT NULL COMMENT '旧版本id',
  `new_build_no` int DEFAULT NULL COMMENT '新版本编译号',
  `old_build_no` int DEFAULT NULL COMMENT '旧版本编译号',
  `direction` enum('forward','rollback') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'forward:发布版本；rollback:回滚版本',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_system_module_env_new_version` (`app_system_id`,`app_module_id`,`env_id`,`new_version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='环境版本变更记录';

-- ----------------------------
-- Table structure for deploy_instance_version
-- ----------------------------
CREATE TABLE `deploy_instance_version` (
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `resource_id` bigint NOT NULL COMMENT '实例id',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `build_no` int DEFAULT NULL COMMENT '编译号',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`app_system_id`,`app_module_id`,`env_id`,`resource_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布实例当前版本';

-- ----------------------------
-- Table structure for deploy_instance_version_audit
-- ----------------------------
CREATE TABLE `deploy_instance_version_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_module_id` bigint NOT NULL COMMENT '模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `resource_id` bigint NOT NULL COMMENT '实例id',
  `new_version_id` bigint NOT NULL COMMENT '新版本id',
  `old_version_id` bigint DEFAULT NULL COMMENT '旧版本id',
  `new_build_no` int DEFAULT NULL COMMENT '新版本编译号',
  `old_build_no` int DEFAULT NULL COMMENT '旧版本编译号',
  `direction` enum('forward','rollback') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'forward:发布版本；rollback:回滚版本',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_system_module_env_resource_new_version` (`app_system_id`,`app_module_id`,`env_id`,`resource_id`,`new_version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实例版本变更记录';

-- ----------------------------
-- Table structure for deploy_job
-- ----------------------------
CREATE TABLE `deploy_job` (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NOT NULL COMMENT '系统id',
  `app_module_id` bigint NOT NULL COMMENT '系统模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `version_id` bigint DEFAULT NULL COMMENT '版本id',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本号',
  `scenario_id` bigint DEFAULT NULL COMMENT '场景id',
  `runner_map_id` bigint DEFAULT NULL COMMENT '编译|构造的runner_id',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `build_no` int DEFAULT NULL COMMENT '编译号',
  `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置hash',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_id` (`app_system_id`) USING BTREE,
  KEY `idx_module_id` (`app_module_id`) USING BTREE,
  KEY `idx_appId_modueId_version_jobId` (`app_system_id`,`app_module_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业表';

-- ----------------------------
-- Table structure for deploy_job_auth
-- ----------------------------
CREATE TABLE `deploy_job_auth` (
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授权对象',
  `type` enum('user','team','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授权类型',
  KEY `idx_job_id` (`job_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业授权';

-- ----------------------------
-- Table structure for deploy_job_content
-- ----------------------------
CREATE TABLE `deploy_job_content` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置hash',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业配置hash';

-- ----------------------------
-- Table structure for deploy_job_lane
-- ----------------------------
CREATE TABLE `deploy_job_lane` (
  `id` bigint NOT NULL COMMENT '批量作业泳道id',
  `batch_job_id` bigint DEFAULT NULL COMMENT '批量作业id',
  `sort` int DEFAULT NULL COMMENT '排序',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业泳道表';

-- ----------------------------
-- Table structure for deploy_job_lane_group
-- ----------------------------
CREATE TABLE `deploy_job_lane_group` (
  `id` bigint NOT NULL COMMENT '组id',
  `lane_id` bigint DEFAULT NULL COMMENT '泳道id',
  `need_wait` tinyint(1) DEFAULT NULL COMMENT '是否需要等待',
  `sort` int DEFAULT NULL COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='批量作业泳道组表';

-- ----------------------------
-- Table structure for deploy_job_lane_group_job
-- ----------------------------
CREATE TABLE `deploy_job_lane_group_job` (
  `group_id` bigint NOT NULL COMMENT '泳道组id',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `sort` int DEFAULT NULL COMMENT '顺序',
  PRIMARY KEY (`group_id`,`job_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='批量作业泳道组作业关联表';

-- ----------------------------
-- Table structure for deploy_job_notify_policy
-- ----------------------------
CREATE TABLE `deploy_job_notify_policy` (
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `notify_policy_id` bigint NOT NULL COMMENT '通知策略id',
  PRIMARY KEY (`app_system_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发布作业引用通知策略表';

-- ----------------------------
-- Table structure for deploy_job_trigger
-- ----------------------------
CREATE TABLE `deploy_job_trigger` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `integration_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集成uuid',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业类型',
  `pipeline_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流水线类型',
  `build_no_policy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编译号策略:the_same :和原作业一致 new:新建buildNo',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一次修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布触发器';

-- ----------------------------
-- Table structure for deploy_job_webhook
-- ----------------------------
CREATE TABLE `deploy_job_webhook` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `integration_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集成uuid',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业类型',
  `pipeline_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流水线类型',
  `build_no_policy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编译号策略',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一次修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布触发器';

-- ----------------------------
-- Table structure for deploy_job_webhook_app_module
-- ----------------------------
CREATE TABLE `deploy_job_webhook_app_module` (
  `webhook_id` bigint NOT NULL COMMENT '触发器id',
  `app_system_id` bigint NOT NULL COMMENT '应用系统id',
  `app_module_id` bigint NOT NULL COMMENT '应用模块id',
  PRIMARY KEY (`webhook_id`,`app_system_id`,`app_module_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for deploy_job_webhook_audit
-- ----------------------------
CREATE TABLE `deploy_job_webhook_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `webhook_id` bigint DEFAULT NULL COMMENT '触发器id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `integration_audit_id` bigint DEFAULT NULL COMMENT '集成记录id',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '触发事件',
  `from_job_id` bigint DEFAULT NULL COMMENT '来源作业id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_triggerid` (`webhook_id`) USING BTREE,
  KEY `idx_integration_autditid` (`integration_audit_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作业触发器记录表';

-- ----------------------------
-- Table structure for deploy_package
-- ----------------------------
CREATE TABLE `deploy_package` (
  `id` bigint NOT NULL COMMENT '主键',
  `group_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工程组标识，在一个组织或者项目中通常是唯一的',
  `artifact_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工程标识，通常是工程的名称，groupId 和 artifact_id 一起定义了 artifact在仓库中的位置',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工程版本号',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'jar' COMMENT '包类型，默认为jar',
  `license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版权许可，开源协议等',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '包在maven仓库中的地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_pkg` (`group_id`,`artifact_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布包信息';

-- ----------------------------
-- Table structure for deploy_pipeline
-- ----------------------------
CREATE TABLE `deploy_pipeline` (
  `id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint DEFAULT NULL,
  `fcd` timestamp(3) NULL DEFAULT NULL,
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lcd` timestamp(3) NULL DEFAULT NULL,
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('appsystem','global') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `app_system_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='超级流水线';

-- ----------------------------
-- Table structure for deploy_pipeline_auth
-- ----------------------------
CREATE TABLE `deploy_pipeline_auth` (
  `pipeline_id` bigint DEFAULT NULL COMMENT '作业id',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授权对象',
  `type` enum('user','team','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授权类型',
  KEY `idx_job_id` (`pipeline_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='超级流水线授权';

-- ----------------------------
-- Table structure for deploy_pipeline_group
-- ----------------------------
CREATE TABLE `deploy_pipeline_group` (
  `id` bigint NOT NULL COMMENT '组id',
  `lane_id` bigint DEFAULT NULL COMMENT '泳道id',
  `need_wait` tinyint(1) DEFAULT NULL COMMENT '是否需要等待',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='批量作业泳道组表';

-- ----------------------------
-- Table structure for deploy_pipeline_jobtemplate
-- ----------------------------
CREATE TABLE `deploy_pipeline_jobtemplate` (
  `id` bigint NOT NULL COMMENT 'id',
  `group_id` bigint DEFAULT NULL COMMENT '分组id',
  `app_system_id` bigint NOT NULL COMMENT '系统id',
  `app_module_id` bigint NOT NULL COMMENT '系统模块id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `scenario_id` bigint DEFAULT NULL COMMENT '场景id',
  `round_count` int DEFAULT NULL COMMENT '分批数量',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_id` (`app_system_id`) USING BTREE,
  KEY `idx_module_id` (`app_module_id`) USING BTREE,
  KEY `idx_appId_modueId_version_jobId` (`app_system_id`,`app_module_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布超级流水线作业模板表';

-- ----------------------------
-- Table structure for deploy_pipeline_lane
-- ----------------------------
CREATE TABLE `deploy_pipeline_lane` (
  `id` bigint NOT NULL COMMENT '流水线泳道id',
  `pipeline_id` bigint DEFAULT NULL COMMENT '流水线id',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布作业泳道超级流水线表';

-- ----------------------------
-- Table structure for deploy_schedule
-- ----------------------------
CREATE TABLE `deploy_schedule` (
  `id` bigint NOT NULL COMMENT '全局唯一id，跨环境导入用',
  `uuid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一uuid',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `begin_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `cron` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'corn表达式',
  `is_active` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0:禁用，1:激活',
  `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行配置信息',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改用户',
  `type` enum('general','pipeline') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作业类型',
  `app_system_id` bigint DEFAULT NULL COMMENT '应用id',
  `app_module_id` bigint DEFAULT NULL COMMENT '模块id',
  `pipeline_id` bigint DEFAULT NULL COMMENT '流水线id',
  `pipeline_type` enum('appsystem','global') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流水线类型',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时作业信息表';

-- ----------------------------
-- Table structure for deploy_sql_detail
-- ----------------------------
CREATE TABLE `deploy_sql_detail` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `system_id` bigint DEFAULT NULL COMMENT '系统 id',
  `module_id` bigint DEFAULT NULL COMMENT '模块 id',
  `env_id` bigint DEFAULT NULL COMMENT '环境 id',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  `status` enum('pending','running','aborting','aborted','succeed','failed','ignored','waitInput') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `sql_file` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件名称',
  `md5` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件md5',
  `host` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ip',
  `port` int DEFAULT NULL COMMENT '端口',
  `resource_id` bigint DEFAULT NULL COMMENT '资产 id',
  `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业节点名',
  `is_delete` tinyint DEFAULT NULL COMMENT '是否删除',
  `runner_id` bigint DEFAULT NULL COMMENT 'runner id',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `node_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点类型',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `sort` int DEFAULT NULL COMMENT '排序',
  `is_modified` int DEFAULT NULL COMMENT '是否改动',
  `warn_count` int DEFAULT NULL COMMENT '告警个数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_system_id_module_id_env_id_version_sql_file` (`system_id`,`module_id`,`env_id`,`version`,`sql_file`,`resource_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布sql状态表';

-- ----------------------------
-- Table structure for deploy_sql_job_phase
-- ----------------------------
CREATE TABLE `deploy_sql_job_phase` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `job_id` bigint DEFAULT NULL COMMENT '作业 id',
  `sql_id` bigint DEFAULT NULL COMMENT '执行sql详情 id',
  `job_phase_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业剧本名',
  `job_phase_id` bigint DEFAULT NULL COMMENT '作业剧本id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ux_job_id_sql_id_job_phase_name` (`job_id`,`sql_id`,`job_phase_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布sql与作业剧本关系表';

-- ----------------------------
-- Table structure for deploy_type_status
-- ----------------------------
CREATE TABLE `deploy_type_status` (
  `type_id` bigint NOT NULL COMMENT '工具类型id',
  `is_active` int NOT NULL COMMENT '是否激活(0:禁用，1：激活)',
  PRIMARY KEY (`type_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布工具类型状态表';

-- ----------------------------
-- Table structure for deploy_version
-- ----------------------------
CREATE TABLE `deploy_version` (
  `id` bigint NOT NULL COMMENT '主键',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版本',
  `app_system_id` bigint NOT NULL COMMENT '应用id',
  `app_system_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用名称',
  `app_module_id` bigint NOT NULL COMMENT '应用模块id',
  `app_module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用模块名称',
  `is_freeze` tinyint NOT NULL COMMENT '是否封版',
  `runner_map_id` bigint DEFAULT NULL COMMENT 'runner映射id',
  `runner_group` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'runner组',
  `compile_success_count` int DEFAULT NULL COMMENT '编译成功次数',
  `compile_fail_count` int DEFAULT NULL COMMENT '编译失败次数',
  `repo_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '仓库类型',
  `repo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '仓库地址',
  `trunk` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主干',
  `branch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分支',
  `tag` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签',
  `tags_dir` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签目录',
  `start_rev` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始Rev号',
  `end_rev` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束Rev号',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_app_system_id_app_module_id_version` (`version`,`app_system_id`,`app_module_id`) USING BTREE,
  KEY `id_is_locked` (`is_freeze`) USING BTREE,
  KEY `idx_fcd` (`fcd`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布版本表';

-- ----------------------------
-- Table structure for deploy_version_appbuild_credential
-- ----------------------------
CREATE TABLE `deploy_version_appbuild_credential` (
  `proxy_to_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '跳转url',
  `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '跳转认证用户',
  PRIMARY KEY (`proxy_to_url`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布版本应用系统编译认证关联表';

-- ----------------------------
-- Table structure for deploy_version_build_quality
-- ----------------------------
CREATE TABLE `deploy_version_build_quality` (
  `id` bigint NOT NULL COMMENT 'id',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `build_time` timestamp(3) NULL DEFAULT NULL COMMENT '编译时间',
  `files` int DEFAULT NULL COMMENT '文件数',
  `classes` int DEFAULT NULL COMMENT '类数',
  `lines` int DEFAULT NULL COMMENT '物理行数（回车数）',
  `ncloc` int DEFAULT NULL COMMENT '代码行数',
  `functions` int DEFAULT NULL COMMENT '函数个数',
  `statements` int DEFAULT NULL COMMENT '语句数',
  `complexity` int DEFAULT NULL COMMENT '复杂度',
  `file_complexity` int DEFAULT NULL COMMENT '文件复杂度',
  `class_complexity` int DEFAULT NULL COMMENT '类复杂度',
  `function_complexity` int DEFAULT NULL COMMENT '函数复杂度',
  `violations` int DEFAULT NULL COMMENT '违规总数',
  `blocker_violations` int DEFAULT NULL COMMENT '阻碍性违规',
  `critical_violations` int DEFAULT NULL COMMENT '严重违规',
  `major_violations` int DEFAULT NULL COMMENT '主要违规',
  `minor_violations` int DEFAULT NULL COMMENT '次要违规',
  `executable_lines_data` int DEFAULT NULL COMMENT '可执行行数据',
  `it_conditions_to_cover` int DEFAULT NULL COMMENT 'it_conditions_to_cover',
  `it_branch_coverage` int DEFAULT NULL COMMENT 'it_branch_coverage',
  `it_conditions_by_line` int DEFAULT NULL COMMENT 'it_conditions_by_line',
  `it_coverage` int DEFAULT NULL COMMENT 'it_coverage',
  `it_coverage_line_hits_data` int DEFAULT NULL COMMENT 'it_coverage_line_hits_data',
  `it_covered_conditions_by_line` int DEFAULT NULL COMMENT 'it_covered_conditions_by_line',
  `it_line_coverage` int DEFAULT NULL COMMENT 'it_line_coverage',
  `it_lines_to_cover` int DEFAULT NULL COMMENT 'it_lines_to_cover',
  `comment_lines_density` double(5,1) DEFAULT NULL COMMENT '行注释 (%)',
  `public_documented_api_density` int DEFAULT NULL COMMENT '添加注释的公有API占比',
  `duplicated_files` int DEFAULT NULL COMMENT '重复文件',
  `duplicated_lines` int DEFAULT NULL COMMENT '重复行',
  `duplicated_lines_density` double(5,1) DEFAULT NULL COMMENT '重复行 (%)',
  `new_duplicated_lines` int DEFAULT NULL COMMENT '新重复行',
  `new_duplicated_lines_density` int DEFAULT NULL COMMENT '新重复行 (%)',
  `duplicated_blocks` int DEFAULT NULL COMMENT '重复块',
  `new_duplicated_blocks` int DEFAULT NULL COMMENT '新重复块',
  `bugs` int DEFAULT NULL COMMENT '漏洞',
  `vulnerabilities` int DEFAULT NULL COMMENT '缺陷',
  `code_smells` int DEFAULT NULL COMMENT '代码味道',
  `new_security_hotspots` int DEFAULT NULL COMMENT '新代码安全热点',
  `new_security_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新代码安全率',
  `new_security_remediation_effort` int DEFAULT NULL COMMENT '新代码修复工作',
  `new_vulnerabilities` int DEFAULT NULL COMMENT '新代码漏洞',
  `security_hotspots` int DEFAULT NULL COMMENT '安全热点',
  `security_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '安全率',
  `security_remediation_effort` int DEFAULT NULL COMMENT '修复工作',
  `comment_lines` int DEFAULT NULL COMMENT '注释行',
  `ncloc_language_distribution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '每种语言的代码行数',
  `new_lines` int DEFAULT NULL COMMENT '新增代码行数',
  `cognitive_complexity` int DEFAULT NULL COMMENT '认知复杂性',
  `conditions_to_cover` int DEFAULT NULL COMMENT '覆盖的条件',
  `coverage` double(7,2) DEFAULT NULL COMMENT '覆盖率',
  `lines_to_cover` int DEFAULT NULL COMMENT '覆盖行',
  `new_conditions_to_cover` int DEFAULT NULL COMMENT '新代码覆盖的条件',
  `new_coverage` double(7,2) DEFAULT NULL COMMENT '新代码覆盖率',
  `new_lines_to_cover` int DEFAULT NULL COMMENT '新代码覆盖行',
  `new_uncovered_conditions` int DEFAULT NULL COMMENT '新代码未覆盖的条件',
  `new_uncovered_lines` int DEFAULT NULL COMMENT '新代码未覆盖行',
  `uncovered_conditions` int DEFAULT NULL COMMENT '未覆盖的条件',
  `uncovered_lines` int DEFAULT NULL COMMENT '未覆盖行',
  `new_bugs` int DEFAULT NULL COMMENT '新代码错误',
  `new_reliability_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新代码可靠率',
  `reliability_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可靠率',
  `new_code_smells` int DEFAULT NULL COMMENT '新代码异味',
  `new_sqale_debt_ratio` double(7,2) DEFAULT NULL COMMENT '新代码的技术债务比率',
  `new_technical_debt` int DEFAULT NULL COMMENT '新代码的技术债务',
  `sqale_debt_ratio` double(7,2) DEFAULT NULL COMMENT '技术债务比率',
  `sqale_index` int DEFAULT NULL COMMENT '技术债务',
  `sqale_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可维护率',
  `confirmed_issues` int DEFAULT NULL COMMENT '确认问题',
  `false_positive_issues` int DEFAULT NULL COMMENT '误判问题',
  `info_violations` int DEFAULT NULL COMMENT '提示问题',
  `new_blocker_violations` int DEFAULT NULL COMMENT '新代码阻断问题',
  `new_critical_violations` int DEFAULT NULL COMMENT '新代码严重问题',
  `new_info_violations` int DEFAULT NULL COMMENT '新代码提示问题',
  `new_major_violations` int DEFAULT NULL COMMENT '新代码主要问题',
  `new_minor_violations` int DEFAULT NULL COMMENT '新代码次要问题',
  `new_violations` int DEFAULT NULL COMMENT '新违规',
  `open_issues` int DEFAULT NULL COMMENT '开启问题',
  `reopened_issues` int DEFAULT NULL COMMENT '重开问题',
  `wont_fix_issues` int DEFAULT NULL COMMENT '不修复的问题',
  `alert_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '质量阀',
  `quality_gate_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '质量阀详细信息',
  `threshold` bigint DEFAULT NULL COMMENT '漏洞阀值',
  `new_maintainability_rating` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新代码可维护率',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `version_build_time` (`version_id`,`build_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布编译质量';

-- ----------------------------
-- Table structure for deploy_version_buildno
-- ----------------------------
CREATE TABLE `deploy_version_buildno` (
  `version_id` bigint NOT NULL COMMENT '发布版本表关联id',
  `build_no` int NOT NULL COMMENT '编译号',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '编译状态',
  `runner_map_id` bigint NOT NULL DEFAULT '0' COMMENT 'runner映射id',
  `runner_group` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'runner组',
  `compile_start_time` timestamp(3) NULL DEFAULT NULL COMMENT '编译开始时间',
  `compile_end_time` timestamp(3) NULL DEFAULT NULL COMMENT '编译结束时间',
  `end_rev` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束Rev号',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`version_id`,`build_no`) USING BTREE,
  KEY `idx_jobId` (`job_id`) USING BTREE,
  KEY `id_fcd` (`compile_start_time`) USING BTREE,
  KEY `idx_compile_start_time` (`compile_start_time`) USING BTREE,
  KEY `idx_buildno` (`build_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布版本编译序号关联表';

-- ----------------------------
-- Table structure for deploy_version_dependency
-- ----------------------------
CREATE TABLE `deploy_version_dependency` (
  `id` bigint NOT NULL COMMENT '主键',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `package_id` bigint NOT NULL COMMENT '关联deploy_pkg表中id',
  `scope` enum('compile','provided','runtime','test','system','import') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'compile' COMMENT '依赖作用域，默认compile',
  `parent_id` bigint DEFAULT NULL COMMENT '父依赖id，关联deploy_pkg表中id',
  `build_time` timestamp(3) NULL DEFAULT NULL COMMENT '编译时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_version_id_package_id` (`version_id`,`package_id`) USING BTREE,
  KEY `idx_version_id` (`version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布版本依赖';

-- ----------------------------
-- Table structure for deploy_version_deployed_instance
-- ----------------------------
CREATE TABLE `deploy_version_deployed_instance` (
  `id` bigint NOT NULL COMMENT 'id',
  `resource_id` bigint NOT NULL COMMENT '实例id',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `deploy_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布人',
  `deploy_time` timestamp(3) NULL DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_instance_version` (`resource_id`,`version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已发布的版本实例表';

-- ----------------------------
-- Table structure for deploy_version_env
-- ----------------------------
CREATE TABLE `deploy_version_env` (
  `version_id` bigint NOT NULL COMMENT '发布版本表关联id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '环境状态',
  `runner_map_id` bigint NOT NULL COMMENT 'runner映射id',
  `runner_group` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'runner组',
  `build_no` int DEFAULT NULL COMMENT 'build序号',
  `is_mirror` tinyint(1) DEFAULT NULL COMMENT '是否是镜像制品',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`version_id`,`env_id`) USING BTREE,
  KEY `idx_jobId` (`job_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布版本环境关联表';

-- ----------------------------
-- Table structure for deploy_version_unit_test
-- ----------------------------
CREATE TABLE `deploy_version_unit_test` (
  `id` bigint NOT NULL COMMENT 'id',
  `version_id` bigint NOT NULL COMMENT '版本id',
  `build_time` timestamp(3) NULL DEFAULT NULL COMMENT '编译时间',
  `tests` int DEFAULT NULL COMMENT '单元测试总数',
  `test_success_density` int DEFAULT NULL COMMENT '单元测试成功率',
  `test_errors` int DEFAULT NULL COMMENT '单元测试失败数',
  `branch_coverage` int DEFAULT NULL COMMENT '全量代码分支覆盖率',
  `new_branch_coverage` int DEFAULT NULL COMMENT '增量代码分支覆盖率',
  `line_coverage` int DEFAULT NULL COMMENT '全量行覆盖率',
  `new_line_coverage` int DEFAULT NULL COMMENT '增量行覆盖率',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `version_build_time` (`version_id`,`build_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发版版本单元测试';