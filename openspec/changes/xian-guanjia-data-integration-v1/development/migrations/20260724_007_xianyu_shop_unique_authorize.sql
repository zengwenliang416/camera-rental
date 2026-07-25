-- Authorizations are unique by authorize_id, NOT seller_id.
-- Multiple XianGuanJia shops/brands can share one seller_id with different authorize_id.

SET @old_shop_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_shop'
    AND index_name = 'uk_xianyu_shop_tenant_application_external'
);
SET @drop_old_shop_index_sql = IF(
  @old_shop_index_exists > 0,
  'ALTER TABLE `xianyu_shop` DROP INDEX `uk_xianyu_shop_tenant_application_external`',
  'SELECT 1'
);
PREPARE drop_old_shop_index_stmt FROM @drop_old_shop_index_sql;
EXECUTE drop_old_shop_index_stmt;
DEALLOCATE PREPARE drop_old_shop_index_stmt;

SET @authorize_shop_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_shop'
    AND index_name = 'uk_xianyu_shop_tenant_application_authorize'
);
SET @add_authorize_shop_index_sql = IF(
  @authorize_shop_index_exists = 0,
  'ALTER TABLE `xianyu_shop` ADD UNIQUE KEY `uk_xianyu_shop_tenant_application_authorize` (`tenant_id`, `application_id`, `authorize_id`)',
  'SELECT 1'
);
PREPARE add_authorize_shop_index_stmt FROM @add_authorize_shop_index_sql;
EXECUTE add_authorize_shop_index_stmt;
DEALLOCATE PREPARE add_authorize_shop_index_stmt;
