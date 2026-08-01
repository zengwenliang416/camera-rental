-- Allow logistics tracking before a Xianyu order is converted into a rental order.
-- Existing rental-order deliveries remain compatible.

SET @channel_order_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_delivery'
    AND column_name = 'channel_order_id'
);
SET @add_channel_order_column_sql = IF(
  @channel_order_column_exists = 0,
  'ALTER TABLE `rental_delivery` ADD COLUMN `channel_order_id` bigint DEFAULT NULL AFTER `rental_order_id`',
  'SELECT 1'
);
PREPARE add_channel_order_column_stmt FROM @add_channel_order_column_sql;
EXECUTE add_channel_order_column_stmt;
DEALLOCATE PREPARE add_channel_order_column_stmt;

ALTER TABLE `rental_delivery`
  MODIFY COLUMN `rental_order_id` bigint DEFAULT NULL;

SET @channel_order_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_delivery'
    AND index_name = 'idx_rental_delivery_channel_order'
);
SET @add_channel_order_index_sql = IF(
  @channel_order_index_exists = 0,
  'ALTER TABLE `rental_delivery` ADD KEY `idx_rental_delivery_channel_order` (`tenant_id`, `channel_order_id`, `direction`, `package_seq`)',
  'SELECT 1'
);
PREPARE add_channel_order_index_stmt FROM @add_channel_order_index_sql;
EXECUTE add_channel_order_index_stmt;
DEALLOCATE PREPARE add_channel_order_index_stmt;

SET @channel_business_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_delivery'
    AND index_name = 'uk_rental_delivery_channel_business'
);
SET @add_channel_business_index_sql = IF(
  @channel_business_index_exists = 0,
  'ALTER TABLE `rental_delivery` ADD UNIQUE KEY `uk_rental_delivery_channel_business` (`tenant_id`, `channel_order_id`, `direction`, `canonical_carrier_code`, `normalized_waybill_no`)',
  'SELECT 1'
);
PREPARE add_channel_business_index_stmt FROM @add_channel_business_index_sql;
EXECUTE add_channel_business_index_stmt;
DEALLOCATE PREPARE add_channel_business_index_stmt;

INSERT INTO `rental_logistics_carrier_mapping`
  (`tenant_id`, `source_type`, `source_carrier_code`, `canonical_carrier_code`,
   `display_name`, `provider_code`, `provider_carrier_code`, `phone_requirement`,
   `status`, `creator`, `updater`)
SELECT t.id, 'XIANYU', 'SHUNFENG', 'SHUNFENG',
       '顺丰速运', 'KUAIDI100', 'shunfeng', 'OPTIONAL',
       'ENABLED', 'system', 'system'
FROM `system_tenant` t
WHERE t.deleted = b'0'
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `provider_code` = VALUES(`provider_code`),
  `provider_carrier_code` = VALUES(`provider_carrier_code`),
  `phone_requirement` = VALUES(`phone_requirement`),
  `status` = VALUES(`status`),
  `updater` = 'system';

INSERT INTO `rental_logistics_carrier_mapping`
  (`tenant_id`, `source_type`, `source_carrier_code`, `canonical_carrier_code`,
   `display_name`, `provider_code`, `provider_carrier_code`, `phone_requirement`,
   `status`, `creator`, `updater`)
SELECT t.id, 'XIANYU', 'YUANTONG', 'YUANTONG',
       '圆通速递', 'KUAIDI100', 'yuantong', 'OPTIONAL',
       'ENABLED', 'system', 'system'
FROM `system_tenant` t
WHERE t.deleted = b'0'
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `provider_code` = VALUES(`provider_code`),
  `provider_carrier_code` = VALUES(`provider_carrier_code`),
  `phone_requirement` = VALUES(`phone_requirement`),
  `status` = VALUES(`status`),
  `updater` = 'system';
