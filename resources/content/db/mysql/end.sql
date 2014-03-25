DROP TABLE IF EXISTS `config_item`;
CREATE TABLE `config_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `source_version` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_config_item__name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `config_item_status`;
CREATE TABLE `config_item_status` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `requested_version` bigint(19) NOT NULL DEFAULT '0',
  `applied_version` bigint(19) NOT NULL DEFAULT '-1',
  `source_version` varchar(255) DEFAULT NULL,
  `requested_updated` datetime NOT NULL,
  `applied_updated` datetime DEFAULT NULL,
  `agent_id` bigint(19) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_config_item_status_name_agent_id` (`name`,`agent_id`),
  KEY `fk_config_item_agent_id` (`agent_id`),
  KEY `idx_config_item_source_version` (`source_version`),
  CONSTRAINT `fk_config_item__agent_id` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `visible` bit(1) NOT NULL DEFAULT b'1',
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_data_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `process_instance`;
CREATE TABLE `process_instance` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `data` text,
  `priority` int(10) DEFAULT 0,
  `process_name` varchar(128) DEFAULT NULL,
  `resource_type` varchar(128) DEFAULT NULL,
  `resource_id` varchar(128) DEFAULT NULL,
  `result` varchar(128) DEFAULT NULL,
  `exit_reason` varchar(128) DEFAULT NULL,
  `phase` varchar(128) DEFAULT NULL,
  `start_process_server_id` varchar(128) DEFAULT NULL,
  `running_process_server_id` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_start_time` (`start_time`),
  KEY `idx_process_instance_end_time` (`end_time`),
  KEY `idx_process_instance_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `process_execution`;
CREATE TABLE `process_execution` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `process_instance_id` bigint(19) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `log` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_process_execution__uuid` (`uuid`),
  KEY `fk_process_execution_process_instance_id` (`process_instance_id`),
  CONSTRAINT `fk_process_execution_process_instance_id` FOREIGN KEY (`process_instance_id`) REFERENCES `process_instance` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `setting`;
CREATE TABLE `setting` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_setting_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_task_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `task_instance`;
CREATE TABLE `task_instance` (
  `id` bigint(19) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `task_id` bigint(19) NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `exception` varchar(255) DEFAULT NULL,
  `server_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_task_instance_task_id` (`task_id`),
  CONSTRAINT `fk_task_instance__task_id` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `process_instance` ADD INDEX `idx_process_instance_et_rt_ri` ( `end_time` , `resource_type` , `resource_id` );
