-- Keep default order and schedule admin pages on tenant-scoped sort indexes.

SET @order_default_page_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_order'
    AND index_name = 'idx_xianyu_order_admin_default'
);
SET @add_order_default_page_index_sql = IF(
  @order_default_page_index_exists = 0,
  'ALTER TABLE `xianyu_order` ADD KEY `idx_xianyu_order_admin_default` (`tenant_id`, `source_updated_at`, `id`)',
  'SELECT 1'
);
PREPARE add_order_default_page_index_stmt FROM @add_order_default_page_index_sql;
EXECUTE add_order_default_page_index_stmt;
DEALLOCATE PREPARE add_order_default_page_index_stmt;

SET @schedule_default_page_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_schedule'
    AND index_name = 'idx_rental_schedule_admin_default'
);
SET @add_schedule_default_page_index_sql = IF(
  @schedule_default_page_index_exists = 0,
  'ALTER TABLE `rental_schedule` ADD KEY `idx_rental_schedule_admin_default` (`tenant_id`, `occupy_start_date`, `id`)',
  'SELECT 1'
);
PREPARE add_schedule_default_page_index_stmt FROM @add_schedule_default_page_index_sql;
EXECUTE add_schedule_default_page_index_stmt;
DEALLOCATE PREPARE add_schedule_default_page_index_stmt;
