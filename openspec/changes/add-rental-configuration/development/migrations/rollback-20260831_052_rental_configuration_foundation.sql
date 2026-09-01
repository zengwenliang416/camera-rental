-- Destructive rollback. Export new rule and remark-history data before execution.
SET NAMES utf8mb4;

DELETE FROM `system_role_menu` WHERE `menu_id` IN (7110, 7111, 7112);
DELETE FROM `system_menu` WHERE `id` IN (7111, 7112, 7110);

DROP TABLE IF EXISTS `xianyu_order_remark_history`;
DROP TABLE IF EXISTS `rental_channel_product_sku_mapping`;
DROP TABLE IF EXISTS `rental_channel_product_rule`;

ALTER TABLE `rental_order`
  DROP INDEX `idx_rental_order_preparation`,
  DROP COLUMN `preparation_updated_at`,
  DROP COLUMN `preparation_reason_code`,
  DROP COLUMN `preparation_status`;

ALTER TABLE `xianyu_order`
  DROP INDEX `idx_xianyu_order_preparation`,
  DROP INDEX `idx_xianyu_order_exact_model`,
  DROP COLUMN `preparation_updated_at`,
  DROP COLUMN `preparation_reason_code`,
  DROP COLUMN `preparation_status`,
  DROP COLUMN `xianyu_sku_id`,
  DROP COLUMN `xgj_sku_id`,
  DROP COLUMN `xianyu_item_id`,
  DROP COLUMN `xgj_product_id`;

-- New rows intentionally leave ambiguous legacy fields null. Restore the original
-- proven XianGuanJia values before reapplying the old NOT NULL constraints.
UPDATE `xianyu_product_sku`
SET `external_sku_id` = `xgj_sku_id`
WHERE `external_sku_id` IS NULL
  AND `xgj_sku_id` IS NOT NULL;

UPDATE `xianyu_product`
SET `external_product_id` = `xgj_product_id`
WHERE `external_product_id` IS NULL
  AND `xgj_product_id` IS NOT NULL;

ALTER TABLE `xianyu_product_sku`
  DROP INDEX `uk_xianyu_sku_product_xianyu`,
  DROP INDEX `uk_xianyu_sku_product_xgj`,
  DROP COLUMN `xianyu_sku_id`,
  DROP COLUMN `xgj_sku_id`,
  MODIFY COLUMN `external_sku_id` varchar(128) NOT NULL DEFAULT '';

ALTER TABLE `xianyu_product`
  DROP INDEX `uk_xianyu_product_shop_item`,
  DROP INDEX `uk_xianyu_product_shop_xgj`,
  DROP COLUMN `xianyu_item_id`,
  DROP COLUMN `xgj_product_id`,
  MODIFY COLUMN `external_product_id` varchar(128) NOT NULL;

ALTER TABLE `xianyu_shop`
  DROP INDEX `idx_xianyu_shop_user_name`,
  DROP COLUMN `xianyu_user_name`;
