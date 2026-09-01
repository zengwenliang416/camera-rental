SET NAMES utf8mb4;

DELIMITER //
CREATE PROCEDURE `assert_rollback_migration_052`()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND column_name IN (
          'xgj_product_id',
          'xianyu_item_id',
          'xgj_sku_id',
          'xianyu_sku_id',
          'preparation_status',
          'preparation_reason_code',
          'preparation_updated_at',
          'xianyu_user_name'
        )
        AND table_name IN (
          'xianyu_shop',
          'xianyu_product',
          'xianyu_product_sku',
          'xianyu_order',
          'rental_order'
        )) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback left explicit columns';
  END IF;

  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN (
          'rental_channel_product_rule',
          'rental_channel_product_sku_mapping',
          'xianyu_order_remark_history'
        )) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback left configuration tables';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `id` IN (7110, 7111, 7112)) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback left menu seeds';
  END IF;

  IF (SELECT COUNT(*) FROM `system_role_menu`
      WHERE `menu_id` IN (7110, 7111, 7112)) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback left role-menu links';
  END IF;

  IF (SELECT `is_nullable` FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'xianyu_product'
        AND column_name = 'external_product_id') <> 'NO' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product legacy nullability not restored';
  END IF;

  IF (SELECT `is_nullable` FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'xianyu_product_sku'
        AND column_name = 'external_sku_id') <> 'NO' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SKU legacy nullability not restored';
  END IF;

  IF (SELECT `external_product_id` FROM `xianyu_product` WHERE `id` = 106)
      <> 'P-POST' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post-migration product legacy value lost';
  END IF;

  IF (SELECT `external_sku_id` FROM `xianyu_product_sku` WHERE `id` = 203)
      <> 'SKU-POST' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post-migration SKU legacy value lost';
  END IF;
END//
DELIMITER ;

CALL `assert_rollback_migration_052`();
DROP PROCEDURE `assert_rollback_migration_052`;

SELECT 'ROLLBACK_ASSERTIONS_PASS' AS `result`;

