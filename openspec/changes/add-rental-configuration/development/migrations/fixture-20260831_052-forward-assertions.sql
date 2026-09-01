SET NAMES utf8mb4;

DELIMITER //
CREATE PROCEDURE `assert_forward_migration_052`()
BEGIN
  DECLARE duplicate_rejected boolean DEFAULT false;

  IF (SELECT `xgj_product_id` FROM `xianyu_product` WHERE `id` = 100)
      <> 'P-UNIQUE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product XGJ backfill failed';
  END IF;

  IF (SELECT `xianyu_item_id` FROM `xianyu_product` WHERE `id` = 100)
      <> 'ITEM-UNIQUE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'unique item backfill failed';
  END IF;

  IF (SELECT `xianyu_item_id` IS NOT NULL
      FROM `xianyu_product` WHERE `id` = 101) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ambiguous product item was guessed';
  END IF;

  IF (SELECT COUNT(*) FROM `xianyu_product`
      WHERE `id` IN (102, 103) AND `xianyu_item_id` IS NOT NULL) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'reverse ambiguous item was guessed';
  END IF;

  IF (SELECT `xianyu_item_id` FROM `xianyu_product` WHERE `id` = 105)
      <> '9007199254740993' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'numeric item ID lost string precision';
  END IF;

  IF (SELECT CONCAT_WS('|', `xgj_product_id`, `xianyu_item_id`, `xgj_sku_id`)
      FROM `xianyu_order` WHERE `id` = 1000)
      <> 'P-UNIQUE|ITEM-UNIQUE|SKU-UNIQUE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'order goods JSON backfill failed';
  END IF;

  IF (SELECT COUNT(*) FROM `xianyu_order`
      WHERE `id` = 1005
        AND (`xgj_product_id` IS NOT NULL
          OR `xianyu_item_id` IS NOT NULL
          OR `xgj_sku_id` IS NOT NULL)) <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invalid JSON used legacy fallback';
  END IF;

  IF (SELECT `xgj_sku_id` FROM `xianyu_product_sku` WHERE `id` = 200)
      <> 'SKU-UNIQUE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SKU XGJ backfill failed';
  END IF;

  IF (SELECT COUNT(*) FROM `xianyu_order`
      WHERE `preparation_status` <> 'WAITING_RECONCILIATION') <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'channel preparation default failed';
  END IF;

  IF (SELECT COUNT(*) FROM `rental_order`
      WHERE `preparation_status` <> 'WAITING_RECONCILIATION') <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rental preparation default failed';
  END IF;

  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN (
          'rental_channel_product_rule',
          'rental_channel_product_sku_mapping',
          'xianyu_order_remark_history'
        )) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'configuration tables missing';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `id` IN (7110, 7111, 7112)) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'menu seeds missing';
  END IF;

  INSERT INTO `xianyu_product` (
    `id`, `tenant_id`, `shop_id`, `external_product_id`,
    `xgj_product_id`, `xianyu_item_id`, `title`
  ) VALUES (
    106, 1, 10, NULL, 'P-POST', 'ITEM-POST', 'post-migration product'
  );

  INSERT INTO `xianyu_product_sku` (
    `id`, `tenant_id`, `product_id`, `external_sku_id`,
    `xgj_sku_id`, `xianyu_sku_id`, `sku_name`
  ) VALUES (
    203, 1, 106, NULL, 'SKU-POST', 'XY-SKU-POST', 'post-migration sku'
  );

  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `xianyu_product` (
      `tenant_id`, `shop_id`, `external_product_id`,
      `xgj_product_id`, `xianyu_item_id`
    ) VALUES (1, 10, NULL, 'P-POST', 'ITEM-OTHER');
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product XGJ uniqueness not enforced';
  END IF;

  SET duplicate_rejected = false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `xianyu_product` (
      `tenant_id`, `shop_id`, `external_product_id`,
      `xgj_product_id`, `xianyu_item_id`
    ) VALUES (1, 10, NULL, 'P-OTHER', 'ITEM-POST');
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product item uniqueness not enforced';
  END IF;

  SET duplicate_rejected = false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `xianyu_product_sku` (
      `tenant_id`, `product_id`, `external_sku_id`,
      `xgj_sku_id`, `xianyu_sku_id`
    ) VALUES (1, 106, NULL, 'SKU-POST', 'XY-SKU-OTHER');
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SKU XGJ uniqueness not enforced';
  END IF;

  SET duplicate_rejected = false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `xianyu_product_sku` (
      `tenant_id`, `product_id`, `external_sku_id`,
      `xgj_sku_id`, `xianyu_sku_id`
    ) VALUES (1, 106, NULL, 'SKU-OTHER', 'XY-SKU-POST');
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Xianyu SKU uniqueness not enforced';
  END IF;

  INSERT INTO `rental_channel_product_rule` (
    `id`, `tenant_id`, `shop_id`, `xianyu_item_id`,
    `handling_policy`, `mapping_mode`
  ) VALUES (
    1, 1, 10, 'ITEM-RULE', 'PARSE_REMARK', 'MULTI'
  );

  SET duplicate_rejected = false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `rental_channel_product_rule` (
      `tenant_id`, `shop_id`, `xianyu_item_id`,
      `handling_policy`, `mapping_mode`
    ) VALUES (
      1, 10, 'ITEM-RULE', 'SKIP_REMARK', 'SINGLE'
    );
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product rule uniqueness not enforced';
  END IF;

  INSERT INTO `rental_channel_product_sku_mapping` (
    `id`, `tenant_id`, `product_rule_id`, `product_sku_id`,
    `xgj_sku_id`, `xianyu_sku_id`, `device_model_id`
  ) VALUES (
    1, 1, 1, 203, 'SKU-POST', 'XY-SKU-POST', 900
  );

  SET duplicate_rejected = false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_rejected = true;
    INSERT INTO `rental_channel_product_sku_mapping` (
      `tenant_id`, `product_rule_id`, `product_sku_id`,
      `xgj_sku_id`, `xianyu_sku_id`, `device_model_id`
    ) VALUES (
      1, 1, 204, 'SKU-POST', 'XY-SKU-OTHER', 901
    );
  END;
  IF NOT duplicate_rejected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SKU rule uniqueness not enforced';
  END IF;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `tenant_id`
  ) VALUES (
    1, 7110, 1
  );
END//
DELIMITER ;

CALL `assert_forward_migration_052`();
DROP PROCEDURE `assert_forward_migration_052`;

SELECT VERSION() AS `mysql_version`;

SELECT COUNT(*) AS `enabled_shops_missing_user_name`
FROM `xianyu_shop`
WHERE `deleted` = b'0'
  AND `authorization_status` = 'VALID'
  AND (`xianyu_user_name` IS NULL OR `xianyu_user_name` = '');

SELECT COUNT(*) AS `unresolved_order_identifiers`
FROM `xianyu_order`
WHERE `deleted` = b'0'
  AND (`xgj_product_id` IS NULL OR `xianyu_item_id` IS NULL);

SELECT `tenant_id`, `shop_id`, `xianyu_item_id`,
       COUNT(*) AS `duplicate_count`
FROM `rental_channel_product_rule`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`, `shop_id`, `xianyu_item_id`
HAVING COUNT(*) > 1;

SELECT 'FORWARD_ASSERTIONS_PASS' AS `result`;

