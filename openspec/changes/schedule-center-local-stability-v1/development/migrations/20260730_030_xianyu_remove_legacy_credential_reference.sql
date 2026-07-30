-- Remove the legacy environment-variable credential indirection.
-- XianGuanJia credentials are now stored only in the tenant-owned,
-- encrypted xianyu_application.app_secret column.

SET @xianyu_credential_reference_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`columns`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'xianyu_application'
    AND `column_name` = 'credential_reference'
);
SET @xianyu_drop_credential_reference_sql = IF(
  @xianyu_credential_reference_exists > 0,
  'ALTER TABLE `xianyu_application` DROP COLUMN `credential_reference`',
  'SELECT 1'
);
PREPARE xianyu_drop_credential_reference_stmt FROM @xianyu_drop_credential_reference_sql;
EXECUTE xianyu_drop_credential_reference_stmt;
DEALLOCATE PREPARE xianyu_drop_credential_reference_stmt;
