-- Make stale PROCESSING webhook events safely reclaimable after worker crashes.

SET @push_processing_token_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_push_event'
    AND column_name = 'processing_token'
);
SET @add_push_processing_token_sql = IF(
  @push_processing_token_exists = 0,
  'ALTER TABLE `xianyu_push_event` ADD COLUMN `processing_token` varchar(64) DEFAULT NULL AFTER `processing_status`',
  'SELECT 1'
);
PREPARE add_push_processing_token_stmt FROM @add_push_processing_token_sql;
EXECUTE add_push_processing_token_stmt;
DEALLOCATE PREPARE add_push_processing_token_stmt;
