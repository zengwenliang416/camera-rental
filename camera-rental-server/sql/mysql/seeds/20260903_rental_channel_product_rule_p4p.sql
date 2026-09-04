-- 2026-09-03 controlled production repair:
-- Map Xianyu item 1062376132143 in tenant 1 / shop 3 to the enabled P4P model,
-- then reconcile the bounded historical set xianyu_order.id <= 2989.
--
-- The caller must set this sentinel in the same MySQL session:
--
--   SET @p4p_rule_repair_confirmation = 'APPLY_1062376132143_TO_P4P';
--   SET @p4p_rule_repair_dry_run = 1; -- optional full rollback preview
--
-- This seed is intentionally fail-closed. It does not update assigned,
-- canceled, settled, deleted, closed, or post-snapshot channel orders.
SET NAMES utf8mb4;

SET @p4p_rule_repair_failed = 0;
SET @p4p_rule_repair_error = NULL;

DROP PROCEDURE IF EXISTS `repair_xianyu_item_1062376132143_to_p4p`;

DELIMITER $$

CREATE PROCEDURE `repair_xianyu_item_1062376132143_to_p4p`()
BEGIN
  DECLARE target_tenant_id bigint DEFAULT 1;
  DECLARE target_shop_id bigint DEFAULT 3;
  DECLARE target_xianyu_item_id varchar(128)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    DEFAULT '1062376132143';
  DECLARE target_xgj_product_id varchar(128)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    DEFAULT '1639893719861829';
  DECLARE target_end_order_id bigint DEFAULT 2989;
  DECLARE target_model_id bigint;
  DECLARE target_product_title varchar(512)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
  DECLARE active_product_count int DEFAULT 0;
  DECLARE target_model_count int DEFAULT 0;
  DECLARE existing_rule_count int DEFAULT 0;
  DECLARE conflicting_rule_count int DEFAULT 0;
  DECLARE candidate_count int DEFAULT 0;
  DECLARE candidate_item_count int DEFAULT 0;
  DECLARE immutable_candidate_count int DEFAULT 0;
  DECLARE closed_ineligible_count int DEFAULT 0;
  DECLARE ready_count int DEFAULT 0;
  DECLARE waiting_remark_count int DEFAULT 0;
  DECLARE changed_item_count int DEFAULT 0;
  DECLARE changed_order_count int DEFAULT 0;
  DECLARE changed_source_count int DEFAULT 0;
  DECLARE dry_run boolean DEFAULT FALSE;
  DECLARE repair_time datetime;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    GET DIAGNOSTICS CONDITION 1
      @p4p_rule_repair_error = MESSAGE_TEXT;
    ROLLBACK;
    SET @p4p_rule_repair_failed = 1;
  END;

  IF BINARY COALESCE(@p4p_rule_repair_confirmation, '') <>
      BINARY 'APPLY_1062376132143_TO_P4P' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'P4P repair confirmation sentinel is missing or invalid';
  END IF;

  SET dry_run = COALESCE(@p4p_rule_repair_dry_run, 0) = 1;

  SELECT COUNT(*), MIN(`title`)
    INTO active_product_count, target_product_title
  FROM `xianyu_product`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `xgj_product_id` = target_xgj_product_id
    AND `deleted` = b'0';

  IF active_product_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected exactly one active target Xianyu product';
  END IF;

  SELECT COUNT(*), MIN(`id`)
    INTO target_model_count, target_model_id
  FROM `rental_device_model`
  WHERE `tenant_id` = target_tenant_id
    AND UPPER(TRIM(`model_code`)) = 'P4P'
    AND `enabled` = b'1'
    AND `deleted` = b'0';

  IF target_model_count <> 1 OR target_model_id <> 21 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected enabled tenant-1 P4P model id 21';
  END IF;

  SELECT COUNT(*)
    INTO existing_rule_count
  FROM `rental_channel_product_rule`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `deleted` = b'0';

  SELECT COUNT(*)
    INTO conflicting_rule_count
  FROM `rental_channel_product_rule`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `deleted` = b'0'
    AND NOT (
      `xgj_product_id` = target_xgj_product_id
      AND `handling_policy` = 'CREATE_RENTAL'
      AND `mapping_mode` = 'SINGLE'
      AND `single_device_model_id` = target_model_id
      AND `enabled` = b'1'
    );

  IF existing_rule_count > 1 OR conflicting_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'A conflicting product rule already exists';
  END IF;

  SELECT COUNT(*)
    INTO candidate_count
  FROM `xianyu_order` source_order
  JOIN `rental_order` rental_order
    ON rental_order.`id` = source_order.`rental_order_id`
   AND rental_order.`tenant_id` = source_order.`tenant_id`
   AND rental_order.`deleted` = b'0'
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`deleted` = b'0'
    AND source_order.`rental_order_id` IS NOT NULL
    AND source_order.`conversion_status` <> 'CLOSED'
    AND NOT EXISTS (
      SELECT 1
      FROM `rental_device_assignment` assignment
      WHERE assignment.`tenant_id` = source_order.`tenant_id`
        AND assignment.`rental_order_id` = source_order.`rental_order_id`
        AND assignment.`deleted` = b'0'
    );

  SELECT COUNT(*)
    INTO candidate_item_count
  FROM `xianyu_order` source_order
  JOIN `rental_order` rental_order
    ON rental_order.`id` = source_order.`rental_order_id`
   AND rental_order.`tenant_id` = source_order.`tenant_id`
   AND rental_order.`deleted` = b'0'
  JOIN `rental_order_item` rental_item
    ON rental_item.`rental_order_id` = rental_order.`id`
   AND rental_item.`tenant_id` = rental_order.`tenant_id`
   AND rental_item.`deleted` = b'0'
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`deleted` = b'0'
    AND source_order.`rental_order_id` IS NOT NULL
    AND source_order.`conversion_status` <> 'CLOSED'
    AND NOT EXISTS (
      SELECT 1
      FROM `rental_device_assignment` assignment
      WHERE assignment.`tenant_id` = source_order.`tenant_id`
        AND assignment.`rental_order_id` = source_order.`rental_order_id`
        AND assignment.`deleted` = b'0'
    );

  SELECT COUNT(*)
    INTO immutable_candidate_count
  FROM `xianyu_order` source_order
  JOIN `rental_order` rental_order
    ON rental_order.`id` = source_order.`rental_order_id`
   AND rental_order.`tenant_id` = source_order.`tenant_id`
   AND rental_order.`deleted` = b'0'
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`deleted` = b'0'
    AND (
      rental_order.`settled_at` IS NOT NULL
      OR rental_order.`status` = 'CANCELED'
      OR EXISTS (
        SELECT 1
        FROM `rental_device_assignment` assignment
        WHERE assignment.`tenant_id` = source_order.`tenant_id`
          AND assignment.`rental_order_id` = source_order.`rental_order_id`
          AND assignment.`deleted` = b'0'
      )
    );

  SELECT COUNT(*)
    INTO closed_ineligible_count
  FROM `xianyu_order`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `id` <= target_end_order_id
    AND `deleted` = b'0'
    AND `rental_order_id` IS NULL
    AND `conversion_status` = 'INELIGIBLE'
    AND `preparation_status` = 'INELIGIBLE'
    AND `preparation_reason_code` = 'ORDER_CLOSED';

  IF candidate_count <> 21 OR candidate_item_count <> 21
      OR immutable_candidate_count <> 0 OR closed_ineligible_count <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'P4P repair candidate snapshot no longer matches reviewed scope';
  END IF;

  SET repair_time = TIMESTAMPADD(HOUR, 8, UTC_TIMESTAMP());

  START TRANSACTION;

  IF existing_rule_count = 0 THEN
    INSERT INTO `rental_channel_product_rule` (
      `tenant_id`,
      `shop_id`,
      `xianyu_item_id`,
      `xgj_product_id`,
      `product_title_snapshot`,
      `handling_policy`,
      `mapping_mode`,
      `single_device_model_id`,
      `enabled`,
      `rule_note`,
      `lock_version`,
      `creator`,
      `updater`
    ) VALUES (
      target_tenant_id,
      target_shop_id,
      target_xianyu_item_id,
      target_xgj_product_id,
      target_product_title,
      'CREATE_RENTAL',
      'SINGLE',
      target_model_id,
      b'1',
      '2026-09-03 controlled P4P mapping and bounded historical reconciliation',
      0,
      'system',
      'system'
    );
  END IF;

  UPDATE `rental_order_item` rental_item
  JOIN `rental_order` rental_order
    ON rental_order.`id` = rental_item.`rental_order_id`
   AND rental_order.`tenant_id` = rental_item.`tenant_id`
   AND rental_order.`deleted` = b'0'
  JOIN `xianyu_order` source_order
    ON source_order.`rental_order_id` = rental_order.`id`
   AND source_order.`tenant_id` = rental_order.`tenant_id`
   AND source_order.`deleted` = b'0'
  SET rental_item.`equipment_model_code` = 'P4P',
      rental_item.`source_product_id` = source_order.`xianyu_item_id`,
      rental_item.`source_sku_id` = NULLIF(TRIM(source_order.`xgj_sku_id`), ''),
      rental_item.`billable_start_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`billable_start_date`
        ELSE rental_item.`billable_start_date`
      END,
      rental_item.`billable_end_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`billable_end_date`
        ELSE rental_item.`billable_end_date`
      END,
      rental_item.`occupy_start_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`ship_date`
        ELSE rental_item.`occupy_start_date`
      END,
      rental_item.`occupy_end_date_exclusive` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN DATE_ADD(source_order.`return_date`, INTERVAL 1 DAY)
        ELSE rental_item.`occupy_end_date_exclusive`
      END,
      rental_item.`expected_send_back_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`return_date`
        ELSE rental_item.`expected_send_back_date`
      END,
      rental_item.`updater` = 'system',
      rental_item.`update_time` = repair_time
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`rental_order_id` IS NOT NULL
    AND source_order.`conversion_status` <> 'CLOSED'
    AND NOT EXISTS (
      SELECT 1
      FROM `rental_device_assignment` assignment
      WHERE assignment.`tenant_id` = source_order.`tenant_id`
        AND assignment.`rental_order_id` = source_order.`rental_order_id`
        AND assignment.`deleted` = b'0'
    );
  SET changed_item_count = ROW_COUNT();

  UPDATE `rental_order` rental_order
  JOIN `xianyu_order` source_order
    ON source_order.`rental_order_id` = rental_order.`id`
   AND source_order.`tenant_id` = rental_order.`tenant_id`
   AND source_order.`deleted` = b'0'
  JOIN `rental_order_item` rental_item
    ON rental_item.`rental_order_id` = rental_order.`id`
   AND rental_item.`tenant_id` = rental_order.`tenant_id`
   AND rental_item.`deleted` = b'0'
  SET rental_order.`billable_start_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`billable_start_date`
        ELSE rental_order.`billable_start_date`
      END,
      rental_order.`billable_end_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`billable_end_date`
        ELSE rental_order.`billable_end_date`
      END,
      rental_order.`occupy_start_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`ship_date`
        ELSE rental_order.`occupy_start_date`
      END,
      rental_order.`occupy_end_date_exclusive` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN DATE_ADD(source_order.`return_date`, INTERVAL 1 DAY)
        ELSE rental_order.`occupy_end_date_exclusive`
      END,
      rental_order.`expected_send_back_date` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`return_date`
        ELSE rental_order.`expected_send_back_date`
      END,
      rental_order.`conversion_version` = CASE
        WHEN source_order.`rental_period_status` = 'SUCCESS'
          THEN source_order.`remark_parse_version`
        ELSE rental_order.`conversion_version`
      END,
      rental_order.`preparation_status` = CASE
        WHEN rental_item.`billable_start_date` IS NOT NULL
          AND rental_item.`billable_end_date` IS NOT NULL
          AND rental_item.`occupy_start_date` IS NOT NULL
          AND rental_item.`occupy_end_date_exclusive` IS NOT NULL
          AND rental_item.`occupy_start_date` < rental_item.`occupy_end_date_exclusive`
          THEN 'READY'
        ELSE 'WAITING_REMARK'
      END,
      rental_order.`preparation_reason_code` = CASE
        WHEN rental_item.`billable_start_date` IS NOT NULL
          AND rental_item.`billable_end_date` IS NOT NULL
          AND rental_item.`occupy_start_date` IS NOT NULL
          AND rental_item.`occupy_end_date_exclusive` IS NOT NULL
          AND rental_item.`occupy_start_date` < rental_item.`occupy_end_date_exclusive`
          THEN NULL
        ELSE COALESCE(
          NULLIF(source_order.`rental_period_reason_code`, ''),
          'RENTAL_PERIOD_NOT_READY'
        )
      END,
      rental_order.`preparation_updated_at` = repair_time,
      rental_order.`updater` = 'system',
      rental_order.`update_time` = repair_time
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`rental_order_id` IS NOT NULL
    AND source_order.`conversion_status` <> 'CLOSED'
    AND NOT EXISTS (
      SELECT 1
      FROM `rental_device_assignment` assignment
      WHERE assignment.`tenant_id` = source_order.`tenant_id`
        AND assignment.`rental_order_id` = source_order.`rental_order_id`
        AND assignment.`deleted` = b'0'
    );
  SET changed_order_count = ROW_COUNT();

  UPDATE `xianyu_order` source_order
  JOIN `rental_order` rental_order
    ON rental_order.`id` = source_order.`rental_order_id`
   AND rental_order.`tenant_id` = source_order.`tenant_id`
   AND rental_order.`deleted` = b'0'
  SET source_order.`preparation_status` = rental_order.`preparation_status`,
      source_order.`preparation_reason_code` = rental_order.`preparation_reason_code`,
      source_order.`preparation_updated_at` = repair_time,
      source_order.`conversion_status` = 'CONVERTED',
      source_order.`updater` = 'system',
      source_order.`update_time` = repair_time
  WHERE source_order.`tenant_id` = target_tenant_id
    AND source_order.`shop_id` = target_shop_id
    AND source_order.`xianyu_item_id` = target_xianyu_item_id
    AND source_order.`id` <= target_end_order_id
    AND source_order.`rental_order_id` IS NOT NULL
    AND source_order.`conversion_status` <> 'CLOSED'
    AND NOT EXISTS (
      SELECT 1
      FROM `rental_device_assignment` assignment
      WHERE assignment.`tenant_id` = source_order.`tenant_id`
        AND assignment.`rental_order_id` = source_order.`rental_order_id`
        AND assignment.`deleted` = b'0'
    );
  SET changed_source_count = ROW_COUNT();

  SELECT COUNT(*)
    INTO ready_count
  FROM `xianyu_order`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `id` <= target_end_order_id
    AND `deleted` = b'0'
    AND `rental_order_id` IS NOT NULL
    AND `preparation_status` = 'READY';

  SELECT COUNT(*)
    INTO waiting_remark_count
  FROM `xianyu_order`
  WHERE `tenant_id` = target_tenant_id
    AND `shop_id` = target_shop_id
    AND `xianyu_item_id` = target_xianyu_item_id
    AND `id` <= target_end_order_id
    AND `deleted` = b'0'
    AND `rental_order_id` IS NOT NULL
    AND `preparation_status` = 'WAITING_REMARK';

  IF ready_count <> 9 OR waiting_remark_count <> 12 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'P4P repair postcondition status counts do not match';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `xianyu_order` source_order
    JOIN `rental_order` rental_order
      ON rental_order.`id` = source_order.`rental_order_id`
     AND rental_order.`tenant_id` = source_order.`tenant_id`
     AND rental_order.`deleted` = b'0'
    JOIN `rental_order_item` rental_item
      ON rental_item.`rental_order_id` = rental_order.`id`
     AND rental_item.`tenant_id` = rental_order.`tenant_id`
     AND rental_item.`deleted` = b'0'
    WHERE source_order.`tenant_id` = target_tenant_id
      AND source_order.`shop_id` = target_shop_id
      AND source_order.`xianyu_item_id` = target_xianyu_item_id
      AND source_order.`id` <= target_end_order_id
      AND source_order.`rental_order_id` IS NOT NULL
      AND (
        rental_item.`equipment_model_code` <> 'P4P'
        OR rental_order.`preparation_status` <> source_order.`preparation_status`
        OR NOT (
          rental_order.`preparation_reason_code`
          <=> source_order.`preparation_reason_code`
        )
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'P4P repair postcondition data mismatch';
  END IF;

  IF dry_run THEN
    ROLLBACK;
  ELSE
    COMMIT;
  END IF;

  SELECT
    IF(
      dry_run,
      'APPLY_1062376132143_TO_P4P_DRY_RUN_PASS',
      'APPLY_1062376132143_TO_P4P_PASS'
    ) AS `repair_status`,
    target_model_id AS `device_model_id`,
    candidate_count AS `candidate_count`,
    changed_item_count AS `changed_item_count`,
    changed_order_count AS `changed_order_count`,
    changed_source_count AS `changed_source_count`,
    ready_count AS `ready_count`,
    waiting_remark_count AS `waiting_remark_count`,
    closed_ineligible_count AS `preserved_closed_count`;
END$$

DELIMITER ;

CALL `repair_xianyu_item_1062376132143_to_p4p`();
DROP PROCEDURE `repair_xianyu_item_1062376132143_to_p4p`;

SELECT COALESCE(
  @p4p_rule_repair_error,
  'APPLY_1062376132143_TO_P4P_PASS'
) AS `repair_status`;

CREATE TEMPORARY TABLE `p4p_rule_repair_guard` (
  `ok` tinyint NOT NULL,
  CONSTRAINT `chk_p4p_rule_repair_guard` CHECK (`ok` = 1)
);

INSERT INTO `p4p_rule_repair_guard` (`ok`)
VALUES (IF(@p4p_rule_repair_failed = 0, 1, 0));

DROP TEMPORARY TABLE `p4p_rule_repair_guard`;
