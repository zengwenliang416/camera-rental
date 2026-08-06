-- Keep status-filtered channel-order pages on the tenant-scoped sort order.

SET @order_status_page_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_order'
    AND index_name = 'idx_xianyu_order_admin_status_page'
);
SET @add_order_status_page_index_sql = IF(
  @order_status_page_index_exists = 0,
  'ALTER TABLE `xianyu_order` ADD KEY `idx_xianyu_order_admin_status_page` (`tenant_id`, `order_status`, `source_updated_at`, `id`)',
  'SELECT 1'
);
PREPARE add_order_status_page_index_stmt FROM @add_order_status_page_index_sql;
EXECUTE add_order_status_page_index_stmt;
DEALLOCATE PREPARE add_order_status_page_index_stmt;
