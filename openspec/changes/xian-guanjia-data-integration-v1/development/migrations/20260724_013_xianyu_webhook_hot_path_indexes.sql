-- Keep webhook seller lookup and stale-event retry scans on bounded composite indexes.

SET @shop_seller_status_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_shop'
    AND index_name = 'idx_xianyu_shop_seller_status'
);
SET @add_shop_seller_status_index_sql = IF(
  @shop_seller_status_index_exists = 0,
  'ALTER TABLE `xianyu_shop` ADD KEY `idx_xianyu_shop_seller_status` (`tenant_id`, `external_shop_id`, `authorization_status`, `id`)',
  'SELECT 1'
);
PREPARE add_shop_seller_status_index_stmt FROM @add_shop_seller_status_index_sql;
EXECUTE add_shop_seller_status_index_stmt;
DEALLOCATE PREPARE add_shop_seller_status_index_stmt;

SET @push_retry_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_push_event'
    AND index_name = 'idx_xianyu_push_event_retry'
);
SET @add_push_retry_index_sql = IF(
  @push_retry_index_exists = 0,
  'ALTER TABLE `xianyu_push_event` ADD KEY `idx_xianyu_push_event_retry` (`tenant_id`, `processing_status`, `update_time`, `id`)',
  'SELECT 1'
);
PREPARE add_push_retry_index_stmt FROM @add_push_retry_index_sql;
EXECUTE add_push_retry_index_stmt;
DEALLOCATE PREPARE add_push_retry_index_stmt;
