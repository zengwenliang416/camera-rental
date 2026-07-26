-- Track which ERP purchase-in item a rental device instance came from (idempotent generate).
-- Safe to re-run: only ADD COLUMN if missing.

SET @db := DATABASE();

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'rental_device' AND column_name = 'source_type'
    ),
    'SELECT 1',
    'ALTER TABLE `rental_device`
       ADD COLUMN `source_type` varchar(32) DEFAULT NULL COMMENT ''来源类型 ERP_PURCHASE_IN 等'' AFTER `enabled`,
       ADD COLUMN `source_biz_id` bigint DEFAULT NULL COMMENT ''来源业务单 ID'' AFTER `source_type`,
       ADD COLUMN `source_item_id` bigint DEFAULT NULL COMMENT ''来源明细 ID'' AFTER `source_biz_id`,
       ADD KEY `idx_rental_device_source` (`tenant_id`, `source_type`, `source_biz_id`, `source_item_id`)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
