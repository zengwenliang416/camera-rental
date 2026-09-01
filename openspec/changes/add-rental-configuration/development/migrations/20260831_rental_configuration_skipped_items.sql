-- Controlled data seed for the approved configuration-skipped Xianyu items.
--
-- This file is intentionally separate from normal migrations. The caller must
-- set both variables in the same MySQL session before sourcing this file:
--
--   SET @rental_configuration_seed_tenant_id = <positive tenant id>;
--   SET @rental_configuration_seed_confirmation =
--     'SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS';
--
-- The seed resolves exactly one active VALID shop named "小疆" and exactly one
-- active VALID shop named "发发" inside that tenant. Any validation or insert
-- error rolls back the entire transaction. Existing rule conflicts are not
-- overwritten.
SET NAMES utf8mb4;

SET @rental_configuration_seed_failed = 0;
SET @rental_configuration_seed_error = NULL;

DROP PROCEDURE IF EXISTS `seed_rental_configuration_skipped_items`;

DELIMITER $$

CREATE PROCEDURE `seed_rental_configuration_skipped_items`()
BEGIN
  DECLARE target_tenant_text varchar(32);
  DECLARE target_tenant_id bigint;
  DECLARE xiaoj_shop_count int DEFAULT 0;
  DECLARE fafa_shop_count int DEFAULT 0;
  DECLARE xiaoj_shop_id bigint;
  DECLARE fafa_shop_id bigint;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    GET DIAGNOSTICS CONDITION 1
      @rental_configuration_seed_error = MESSAGE_TEXT;
    ROLLBACK;
    SET @rental_configuration_seed_failed = 1;
  END;

  IF COALESCE(@rental_configuration_seed_confirmation, '') <>
      'SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Seed confirmation sentinel is missing or invalid';
  END IF;

  SET target_tenant_text =
    TRIM(CAST(@rental_configuration_seed_tenant_id AS CHAR));

  IF target_tenant_text IS NULL
      OR target_tenant_text NOT REGEXP '^[1-9][0-9]*$'
      OR CHAR_LENGTH(target_tenant_text) > 19
      OR (
        CHAR_LENGTH(target_tenant_text) = 19
        AND CAST(target_tenant_text AS DECIMAL(20, 0)) > 9223372036854775807
      ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Seed tenant id must be a positive signed BIGINT';
  END IF;

  SET target_tenant_id = CAST(target_tenant_text AS UNSIGNED);

  SELECT COUNT(*), MIN(`id`)
    INTO xiaoj_shop_count, xiaoj_shop_id
  FROM `xianyu_shop`
  WHERE `tenant_id` = target_tenant_id
    AND TRIM(`shop_name`) = '小疆'
    AND `authorization_status` = 'VALID'
    AND (
      `authorization_expires_at` IS NULL
      OR `authorization_expires_at` > TIMESTAMPADD(HOUR, 8, UTC_TIMESTAMP())
    )
    AND `deleted` = b'0';

  SELECT COUNT(*), MIN(`id`)
    INTO fafa_shop_count, fafa_shop_id
  FROM `xianyu_shop`
  WHERE `tenant_id` = target_tenant_id
    AND TRIM(`shop_name`) = '发发'
    AND `authorization_status` = 'VALID'
    AND (
      `authorization_expires_at` IS NULL
      OR `authorization_expires_at` > TIMESTAMPADD(HOUR, 8, UTC_TIMESTAMP())
    )
    AND `deleted` = b'0';

  IF xiaoj_shop_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected exactly one active VALID 小疆 shop for tenant';
  END IF;

  IF fafa_shop_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected exactly one active VALID 发发 shop for tenant';
  END IF;

  START TRANSACTION;

  INSERT INTO `rental_channel_product_rule` (
    `tenant_id`,
    `shop_id`,
    `xianyu_item_id`,
    `handling_policy`,
    `mapping_mode`,
    `enabled`,
    `rule_note`
  ) VALUES
    (target_tenant_id, xiaoj_shop_id, '1062409679830', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '1061015327345', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '1042851395917', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '1021749783370', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '1015758423054', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '996427340341', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '994967964760', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '980821925580', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '946426581576', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '969348191931', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '964654687997', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '989974832741', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '984580566155', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '930100016211', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '983025637118', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '985707224806', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, xiaoj_shop_id, '986580601148', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '1024163647751', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '1022288043626', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '1018390062846', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '1017700474288', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '1015971948191', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '997210149459', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '994824734648', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '995640812523', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '946905413897', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '977248345425', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '957670857301', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item'),
    (target_tenant_id, fafa_shop_id, '942506886325', 'CONFIG_SKIPPED', 'NONE', b'1', 'Task 002 approved skipped item');

  COMMIT;
END$$

DELIMITER ;

CALL `seed_rental_configuration_skipped_items`();
DROP PROCEDURE `seed_rental_configuration_skipped_items`;

SELECT COALESCE(
  @rental_configuration_seed_error,
  'SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS_PASS'
) AS `seed_status`;

-- Convert a caught validation or DML error into a non-zero mysql client exit
-- only after the helper procedure has been removed.
CREATE TEMPORARY TABLE `rental_configuration_seed_guard` (
  `ok` tinyint NOT NULL,
  CONSTRAINT `chk_rental_configuration_seed_guard` CHECK (`ok` = 1)
);

INSERT INTO `rental_configuration_seed_guard` (`ok`)
VALUES (IF(@rental_configuration_seed_failed = 0, 1, 0));

DROP TEMPORARY TABLE `rental_configuration_seed_guard`;
