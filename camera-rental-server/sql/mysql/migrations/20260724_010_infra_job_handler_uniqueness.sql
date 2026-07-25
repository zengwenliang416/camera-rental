-- Ensure programmatic job registration is idempotent across concurrent application instances.
-- Deleted rows produce NULL and therefore do not block recreating the same handler.

SET @active_handler_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'infra_job'
    AND column_name = 'active_handler_name'
);
SET @add_active_handler_column_sql = IF(
  @active_handler_column_exists = 0,
  'ALTER TABLE `infra_job` ADD COLUMN `active_handler_name` varchar(64) GENERATED ALWAYS AS (IF(`deleted` = b''0'', `handler_name`, NULL)) STORED',
  'SELECT 1'
);
PREPARE add_active_handler_column_stmt FROM @add_active_handler_column_sql;
EXECUTE add_active_handler_column_stmt;
DEALLOCATE PREPARE add_active_handler_column_stmt;

SET @active_handler_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'infra_job'
    AND index_name = 'uk_infra_job_active_handler'
);
SET @add_active_handler_index_sql = IF(
  @active_handler_index_exists = 0,
  'ALTER TABLE `infra_job` ADD UNIQUE KEY `uk_infra_job_active_handler` (`active_handler_name`)',
  'SELECT 1'
);
PREPARE add_active_handler_index_stmt FROM @add_active_handler_index_sql;
EXECUTE add_active_handler_index_stmt;
DEALLOCATE PREPARE add_active_handler_index_stmt;
