ALTER TABLE `deploy_version_cve` ADD  INDEX `idx_version_id_highest_severity` (`version_id`, `highest_severity`);
