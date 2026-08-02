-- Fixed customer return entry lookup and admin wording.
SET NAMES utf8mb4;

SET @return_lookup_index_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'xianyu_order'
    AND `index_name` = 'idx_xianyu_order_external_return_lookup'
);
SET @return_lookup_index_sql = IF(
  @return_lookup_index_exists = 0,
  'CREATE INDEX `idx_xianyu_order_external_return_lookup` ON `xianyu_order` (`external_order_id`, `deleted`, `tenant_id`)',
  'SELECT 1'
);
PREPARE return_lookup_index_stmt FROM @return_lookup_index_sql;
EXECUTE return_lookup_index_stmt;
DEALLOCATE PREPARE return_lookup_index_stmt;

UPDATE `system_menu`
SET `name` = '复制固定退回入口',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7100
  AND `deleted` = b'0';
