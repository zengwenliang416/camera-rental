-- Align live schemas with current raw-payload and push-replay runtime fields.

SET @raw_schema_version_width_ok = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_raw_payload'
    AND column_name = 'schema_version'
    AND character_maximum_length >= 64
);
SET @widen_raw_schema_version_sql = IF(
  @raw_schema_version_width_ok = 0,
  'ALTER TABLE `xianyu_raw_payload` MODIFY COLUMN `schema_version` varchar(64) NOT NULL',
  'SELECT 1'
);
PREPARE widen_raw_schema_version_stmt FROM @widen_raw_schema_version_sql;
EXECUTE widen_raw_schema_version_stmt;
DEALLOCATE PREPARE widen_raw_schema_version_stmt;

SET @raw_redaction_version_width_ok = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_raw_payload'
    AND column_name = 'redaction_version'
    AND character_maximum_length >= 64
);
SET @widen_raw_redaction_version_sql = IF(
  @raw_redaction_version_width_ok = 0,
  'ALTER TABLE `xianyu_raw_payload` MODIFY COLUMN `redaction_version` varchar(64) NOT NULL',
  'SELECT 1'
);
PREPARE widen_raw_redaction_version_stmt FROM @widen_raw_redaction_version_sql;
EXECUTE widen_raw_redaction_version_stmt;
DEALLOCATE PREPARE widen_raw_redaction_version_stmt;

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
