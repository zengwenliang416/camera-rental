-- Allow fixed-entry returns to be recorded before channel-order conversion and device assignment.
SET NAMES utf8mb4;

ALTER TABLE `rental_return_registration`
  MODIFY COLUMN `rental_order_id` bigint DEFAULT NULL COMMENT '内部租赁订单编号，未转换时为空';

SET @return_channel_index_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'rental_return_registration'
    AND `index_name` = 'idx_return_registration_channel'
);
SET @return_channel_index_sql = IF(
  @return_channel_index_exists = 0,
  'CREATE INDEX `idx_return_registration_channel` ON `rental_return_registration` (`tenant_id`, `channel_order_id`, `status`, `id`)',
  'SELECT 1'
);
PREPARE return_channel_index_stmt FROM @return_channel_index_sql;
EXECUTE return_channel_index_stmt;
DEALLOCATE PREPARE return_channel_index_stmt;
