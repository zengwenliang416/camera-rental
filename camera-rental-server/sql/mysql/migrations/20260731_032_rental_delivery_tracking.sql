-- Rental logistics tracking foundation.
-- This migration is additive only and performs no historical backfill or provider call.

CREATE TABLE IF NOT EXISTS `rental_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `rental_order_id` bigint NOT NULL,
  `direction` varchar(32) NOT NULL,
  `package_seq` int NOT NULL DEFAULT 1,
  `source_type` varchar(32) NOT NULL,
  `source_identifier` varchar(128) DEFAULT NULL,
  `source_carrier_code` varchar(64) NOT NULL,
  `source_carrier_name` varchar(128) DEFAULT NULL,
  `canonical_carrier_code` varchar(64) NOT NULL,
  `provider_code` varchar(32) DEFAULT NULL,
  `provider_carrier_code` varchar(64) DEFAULT NULL,
  `provider_credential_id` bigint DEFAULT NULL,
  `waybill_no` varchar(128) NOT NULL,
  `normalized_waybill_no` varchar(128) NOT NULL,
  `tracking_phone` varchar(512) DEFAULT NULL,
  `callback_token` varchar(512) DEFAULT NULL,
  `callback_token_hash` char(64) DEFAULT NULL,
  `callback_salt` varchar(512) DEFAULT NULL,
  `lifecycle_status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `mapping_status` varchar(32) NOT NULL DEFAULT 'MAPPING_REQUIRED',
  `subscribe_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `query_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `tracking_status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `tracking_version` int NOT NULL DEFAULT 0,
  `current_snapshot_hash` char(64) DEFAULT NULL,
  `latest_event_time` datetime DEFAULT NULL,
  `latest_trace_text` varchar(1000) DEFAULT NULL,
  `latest_location` varchar(255) DEFAULT NULL,
  `estimated_delivery_at` datetime DEFAULT NULL,
  `last_synced_at` datetime DEFAULT NULL,
  `last_callback_at` datetime DEFAULT NULL,
  `next_query_allowed_at` datetime DEFAULT NULL,
  `subscribe_month` char(7) DEFAULT NULL,
  `subscribe_count` int NOT NULL DEFAULT 0,
  `next_subscribe_allowed_at` datetime DEFAULT NULL,
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` varchar(255) DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_delivery_business` (
    `tenant_id`, `rental_order_id`, `direction`, `canonical_carrier_code`, `normalized_waybill_no`
  ),
  UNIQUE KEY `uk_rental_delivery_callback_hash` (`tenant_id`, `callback_token_hash`),
  KEY `idx_rental_delivery_callback_hash` (`callback_token_hash`),
  KEY `idx_rental_delivery_order` (`tenant_id`, `rental_order_id`, `direction`, `package_seq`),
  KEY `idx_rental_delivery_tracking` (`tenant_id`, `tracking_status`, `last_synced_at`),
  KEY `idx_rental_delivery_provider` (`tenant_id`, `provider_code`, `subscribe_status`, `query_status`),
  KEY `idx_rental_delivery_credential` (`tenant_id`, `provider_credential_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁真实物流包裹';

SET @provider_credential_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_delivery'
    AND column_name = 'provider_credential_id'
);
SET @add_provider_credential_column_sql = IF(
  @provider_credential_column_exists = 0,
  'ALTER TABLE `rental_delivery` ADD COLUMN `provider_credential_id` bigint DEFAULT NULL AFTER `provider_carrier_code`',
  'SELECT 1'
);
PREPARE add_provider_credential_column_stmt FROM @add_provider_credential_column_sql;
EXECUTE add_provider_credential_column_stmt;
DEALLOCATE PREPARE add_provider_credential_column_stmt;

SET @provider_credential_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_delivery'
    AND index_name = 'idx_rental_delivery_credential'
);
SET @add_provider_credential_index_sql = IF(
  @provider_credential_index_exists = 0,
  'ALTER TABLE `rental_delivery` ADD KEY `idx_rental_delivery_credential` (`tenant_id`, `provider_credential_id`)',
  'SELECT 1'
);
PREPARE add_provider_credential_index_stmt FROM @add_provider_credential_index_sql;
EXECUTE add_provider_credential_index_stmt;
DEALLOCATE PREPARE add_provider_credential_index_stmt;

-- Repair pre-release executions of this migration that created a global
-- callback-token unique key. Public webhook lookup uses the non-unique hash
-- index, while uniqueness remains scoped to one tenant.
SET @callback_hash_unique_columns = (
  SELECT GROUP_CONCAT(`column_name` ORDER BY `seq_in_index` SEPARATOR ',')
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'rental_delivery'
    AND `index_name` = 'uk_rental_delivery_callback_hash'
    AND `non_unique` = 0
);
SET @drop_legacy_callback_hash_index_sql = IF(
  @callback_hash_unique_columns IS NOT NULL
    AND @callback_hash_unique_columns <> 'tenant_id,callback_token_hash',
  'ALTER TABLE `rental_delivery` DROP INDEX `uk_rental_delivery_callback_hash`',
  'SELECT 1'
);
PREPARE drop_legacy_callback_hash_index_stmt FROM @drop_legacy_callback_hash_index_sql;
EXECUTE drop_legacy_callback_hash_index_stmt;
DEALLOCATE PREPARE drop_legacy_callback_hash_index_stmt;

SET @callback_hash_unique_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'rental_delivery'
    AND `index_name` = 'uk_rental_delivery_callback_hash'
);
SET @add_callback_hash_unique_sql = IF(
  @callback_hash_unique_exists = 0,
  'ALTER TABLE `rental_delivery` ADD UNIQUE KEY `uk_rental_delivery_callback_hash` (`tenant_id`, `callback_token_hash`)',
  'SELECT 1'
);
PREPARE add_callback_hash_unique_stmt FROM @add_callback_hash_unique_sql;
EXECUTE add_callback_hash_unique_stmt;
DEALLOCATE PREPARE add_callback_hash_unique_stmt;

SET @callback_hash_lookup_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'rental_delivery'
    AND `index_name` = 'idx_rental_delivery_callback_hash'
);
SET @add_callback_hash_lookup_sql = IF(
  @callback_hash_lookup_exists = 0,
  'ALTER TABLE `rental_delivery` ADD KEY `idx_rental_delivery_callback_hash` (`callback_token_hash`)',
  'SELECT 1'
);
PREPARE add_callback_hash_lookup_stmt FROM @add_callback_hash_lookup_sql;
EXECUTE add_callback_hash_lookup_stmt;
DEALLOCATE PREPARE add_callback_hash_lookup_stmt;

CREATE TABLE IF NOT EXISTS `rental_delivery_device_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `delivery_id` bigint NOT NULL,
  `rental_order_id` bigint NOT NULL,
  `rental_order_item_id` bigint NOT NULL,
  `assignment_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_delivery_device` (`tenant_id`, `delivery_id`, `device_id`),
  UNIQUE KEY `uk_rental_delivery_assignment` (`tenant_id`, `delivery_id`, `assignment_id`),
  KEY `idx_rental_delivery_rel_order` (`tenant_id`, `rental_order_id`, `rental_order_item_id`),
  KEY `idx_rental_delivery_rel_device` (`tenant_id`, `device_id`, `delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流包裹设备关系';

CREATE TABLE IF NOT EXISTS `rental_delivery_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `delivery_id` bigint NOT NULL,
  `snapshot_version` int NOT NULL,
  `snapshot_hash` char(64) NOT NULL,
  `event_seq` int NOT NULL,
  `event_fingerprint` char(64) NOT NULL,
  `business_time` datetime DEFAULT NULL,
  `raw_time` varchar(128) DEFAULT NULL,
  `tracking_status` varchar(32) NOT NULL,
  `provider_status` varchar(64) DEFAULT NULL,
  `trace_text` varchar(1000) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `event_source` varchar(32) NOT NULL,
  `inbox_id` bigint DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_trace_seq` (`tenant_id`, `delivery_id`, `snapshot_version`, `event_seq`),
  UNIQUE KEY `uk_rental_trace_fingerprint` (
    `tenant_id`, `delivery_id`, `snapshot_version`, `event_fingerprint`
  ),
  KEY `idx_rental_trace_current` (`tenant_id`, `delivery_id`, `snapshot_version`, `business_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流完整轨迹快照';

CREATE TABLE IF NOT EXISTS `rental_delivery_callback_inbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `provider_code` varchar(32) NOT NULL,
  `delivery_id` bigint NOT NULL,
  `provider_task_id` varchar(128) DEFAULT NULL,
  `payload_hash` char(64) NOT NULL,
  `callback_params` longtext NOT NULL,
  `processing_status` varchar(32) NOT NULL DEFAULT 'RECEIVED',
  `processing_token` varchar(64) DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime DEFAULT NULL,
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` varchar(255) DEFAULT NULL,
  `received_at` datetime NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_inbox_payload` (`tenant_id`, `provider_code`, `delivery_id`, `payload_hash`),
  KEY `idx_rental_inbox_work` (`tenant_id`, `processing_status`, `next_retry_at`, `lease_until`),
  KEY `idx_rental_inbox_claim` (`tenant_id`, `id`),
  KEY `idx_rental_inbox_delivery` (`tenant_id`, `delivery_id`, `received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流回调收件箱';

CREATE TABLE IF NOT EXISTS `rental_delivery_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `delivery_id` bigint NOT NULL,
  `event_type` varchar(32) NOT NULL,
  `dedupe_key` varchar(128) NOT NULL,
  `safe_metadata` varchar(1000) DEFAULT NULL,
  `processing_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `processing_token` varchar(64) DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_attempt_at` datetime DEFAULT NULL,
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` varchar(255) DEFAULT NULL,
  `scheduled_at` datetime NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_outbox_dedupe` (`tenant_id`, `dedupe_key`),
  KEY `idx_rental_outbox_work` (`tenant_id`, `processing_status`, `next_attempt_at`, `lease_until`),
  KEY `idx_rental_outbox_claim` (`tenant_id`, `id`),
  KEY `idx_rental_outbox_delivery` (`tenant_id`, `delivery_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流异步任务发件箱';

CREATE TABLE IF NOT EXISTS `rental_logistics_carrier_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `source_type` varchar(32) NOT NULL,
  `source_carrier_code` varchar(64) NOT NULL,
  `canonical_carrier_code` varchar(64) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `provider_code` varchar(32) NOT NULL,
  `provider_carrier_code` varchar(64) NOT NULL,
  `phone_requirement` varchar(16) NOT NULL DEFAULT 'OPTIONAL',
  `status` varchar(16) NOT NULL DEFAULT 'DISABLED',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_carrier_source` (`tenant_id`, `source_type`, `source_carrier_code`),
  UNIQUE KEY `uk_rental_carrier_provider` (
    `tenant_id`, `provider_code`, `canonical_carrier_code`, `provider_carrier_code`
  ),
  KEY `idx_rental_carrier_status` (`tenant_id`, `status`, `provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流承运商编码映射';

CREATE TABLE IF NOT EXISTS `rental_logistics_provider_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `provider_code` varchar(32) NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'0',
  `query_enabled` bit(1) NOT NULL DEFAULT b'0',
  `subscribe_enabled` bit(1) NOT NULL DEFAULT b'0',
  `callback_secret` varchar(512) DEFAULT NULL,
  `callback_base_url` varchar(512) DEFAULT NULL,
  `minimum_query_interval_seconds` int NOT NULL DEFAULT 1800,
  `result_version` varchar(16) NOT NULL DEFAULT '4',
  `config_status` varchar(32) NOT NULL DEFAULT 'INCOMPLETE',
  `last_verified_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_provider_config` (`tenant_id`, `provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户物流供应商配置';

CREATE TABLE IF NOT EXISTS `rental_logistics_provider_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `provider_code` varchar(32) NOT NULL,
  `credential_name` varchar(64) NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'0',
  `sort_order` int NOT NULL DEFAULT 100,
  `customer_code` varchar(512) DEFAULT NULL,
  `api_key` varchar(512) DEFAULT NULL,
  `config_status` varchar(32) NOT NULL DEFAULT 'INCOMPLETE',
  `last_verified_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_provider_credential_name` (
    `tenant_id`, `provider_code`, `credential_name`
  ),
  KEY `idx_rental_provider_credential_enabled` (
    `tenant_id`, `provider_code`, `enabled`, `sort_order`, `id`
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户物流供应商多凭据';

-- Upgrade pre-release databases that stored one credential pair directly on
-- the Provider config row. Encrypted values are copied without decryption.
SET @legacy_provider_customer_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_logistics_provider_config'
    AND column_name = 'customer_code'
);
SET @legacy_provider_api_key_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_logistics_provider_config'
    AND column_name = 'api_key'
);
SET @migrate_legacy_provider_credential_sql = IF(
  @legacy_provider_customer_column_exists = 1
    AND @legacy_provider_api_key_column_exists = 1,
  'INSERT IGNORE INTO `rental_logistics_provider_credential` (`tenant_id`, `provider_code`, `credential_name`, `enabled`, `sort_order`, `customer_code`, `api_key`, `config_status`, `creator`, `updater`) SELECT `tenant_id`, `provider_code`, ''default'', `enabled`, 100, `customer_code`, `api_key`, IF(`customer_code` IS NOT NULL AND `api_key` IS NOT NULL, ''READY_UNVERIFIED'', ''INCOMPLETE''), `creator`, `updater` FROM `rental_logistics_provider_config` WHERE `customer_code` IS NOT NULL OR `api_key` IS NOT NULL',
  'SELECT 1'
);
PREPARE migrate_legacy_provider_credential_stmt FROM @migrate_legacy_provider_credential_sql;
EXECUTE migrate_legacy_provider_credential_stmt;
DEALLOCATE PREPARE migrate_legacy_provider_credential_stmt;

SET @delivery_id_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device_shipment'
    AND column_name = 'delivery_id'
);
SET @add_delivery_id_column_sql = IF(
  @delivery_id_column_exists = 0,
  'ALTER TABLE `rental_device_shipment` ADD COLUMN `delivery_id` bigint DEFAULT NULL AFTER `device_id`',
  'SELECT 1'
);
PREPARE add_delivery_id_column_stmt FROM @add_delivery_id_column_sql;
EXECUTE add_delivery_id_column_stmt;
DEALLOCATE PREPARE add_delivery_id_column_stmt;

SET @delivery_id_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device_shipment'
    AND index_name = 'idx_rental_shipment_delivery'
);
SET @add_delivery_id_index_sql = IF(
  @delivery_id_index_exists = 0,
  'ALTER TABLE `rental_device_shipment` ADD KEY `idx_rental_shipment_delivery` (`tenant_id`, `delivery_id`)',
  'SELECT 1'
);
PREPARE add_delivery_id_index_stmt FROM @add_delivery_id_index_sql;
EXECUTE add_delivery_id_index_stmt;
DEALLOCATE PREPARE add_delivery_id_index_stmt;

-- Register grantable logistics permissions under the independent schedule-center access menu.
-- These are permission buttons only. This migration intentionally does not grant them to any role.
INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
  permission_row.`id`, permission_row.`name`, permission_row.`permission`, 3,
  permission_row.`sort`, 7081,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT 7082 AS `id`, '物流跟踪查询' AS `name`, 'rental:delivery:tracking' AS `permission`, 1 AS `sort`
  UNION ALL SELECT 7083, '物流配置查询', 'rental:logistics:config:query', 2
  UNION ALL SELECT 7084, '物流配置更新', 'rental:logistics:config:update', 3
  UNION ALL SELECT 7085, '物流配置验证', 'rental:logistics:config:verify', 4
  UNION ALL SELECT 7086, '承运商映射查询', 'rental:logistics:mapping:query', 5
  UNION ALL SELECT 7087, '承运商映射更新', 'rental:logistics:mapping:update', 6
  UNION ALL SELECT 7088, '承运商映射删除', 'rental:logistics:mapping:delete', 7
  UNION ALL SELECT 7089, '物流失败任务查询', 'rental:logistics:task:query', 8
  UNION ALL SELECT 7090, '物流失败任务重试', 'rental:logistics:task:retry', 9
  UNION ALL SELECT 7091, '物流任务对账', 'rental:logistics:reconcile', 10
  UNION ALL SELECT 7092, '物流指标查询', 'rental:logistics:metrics:query', 11
  UNION ALL SELECT 7093, '物流历史回填', 'rental:logistics:backfill', 12
  UNION ALL SELECT 7094, '物流技术数据清理', 'rental:logistics:cleanup', 13
) permission_row
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_menu` existing_menu
  WHERE existing_menu.`id` = permission_row.`id`
     OR existing_menu.`permission` = permission_row.`permission`
);
