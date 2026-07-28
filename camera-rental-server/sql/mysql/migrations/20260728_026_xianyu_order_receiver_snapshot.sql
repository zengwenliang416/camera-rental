-- Preserve shipping contact after XianGuanJia stops returning it for shipped orders.
-- Raw payloads remain immutable evidence; these columns hold the latest known contact snapshot.

ALTER TABLE `xianyu_order`
  ADD COLUMN `receiver_name` varchar(128) DEFAULT NULL COMMENT '最近一次待发货详情中的收货人' AFTER `detail_json`,
  ADD COLUMN `receiver_mobile` varchar(64) DEFAULT NULL COMMENT '最近一次待发货详情中的收货电话' AFTER `receiver_name`,
  ADD COLUMN `receiver_address` varchar(1024) DEFAULT NULL COMMENT '最近一次待发货详情中的完整收货地址' AFTER `receiver_mobile`;

UPDATE `xianyu_order`
SET
  `receiver_name` = NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.receiver_name')), ''), 'null'),
  `receiver_mobile` = NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.receiver_mobile')), ''), 'null'),
  `receiver_address` = NULLIF(CONCAT_WS('',
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.prov_name')), ''), 'null'),
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.city_name')), ''), 'null'),
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.area_name')), ''), 'null'),
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.town_name')), ''), 'null'),
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`detail_json`, '$.address')), ''), 'null')
  ), '')
WHERE `detail_json` IS NOT NULL
  AND JSON_VALID(`detail_json`);

CREATE TEMPORARY TABLE `tmp_xianyu_order_receiver_snapshot` AS
SELECT `tenant_id`, `shop_id`, `external_order_id`, `receiver_name`, `receiver_mobile`, `receiver_address`
FROM (
  SELECT
    `tenant_id`,
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(`source_identifier`, ':', 2), ':', -1) AS UNSIGNED) AS `shop_id`,
    SUBSTRING_INDEX(`source_identifier`, ':', -1) AS `external_order_id`,
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.receiver_name')), ''), 'null') AS `receiver_name`,
    NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.receiver_mobile')), ''), 'null') AS `receiver_mobile`,
    NULLIF(CONCAT_WS('',
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.prov_name')), ''), 'null'),
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.city_name')), ''), 'null'),
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.area_name')), ''), 'null'),
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.town_name')), ''), 'null'),
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.address')), ''), 'null')
    ), '') AS `receiver_address`,
    ROW_NUMBER() OVER (
      PARTITION BY `tenant_id`, `source_identifier`
      ORDER BY `received_at` DESC, `id` DESC
    ) AS `row_number`
  FROM `xianyu_raw_payload`
  WHERE `source_type` = 'ORDER_DETAIL'
    AND `deleted` = b'0'
    AND `source_identifier` LIKE 'order:%:%'
    AND JSON_VALID(`payload`)
    AND (
      NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.receiver_name')), ''), 'null') IS NOT NULL
      OR NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.receiver_mobile')), ''), 'null') IS NOT NULL
      OR NULLIF(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`payload`, '$.data.address')), ''), 'null') IS NOT NULL
    )
) `ranked`
WHERE `row_number` = 1;

UPDATE `xianyu_order` `orders`
JOIN `tmp_xianyu_order_receiver_snapshot` `snapshot`
  ON `snapshot`.`tenant_id` = `orders`.`tenant_id`
  AND `snapshot`.`shop_id` = `orders`.`shop_id`
  AND `snapshot`.`external_order_id` = `orders`.`external_order_id`
SET
  `orders`.`receiver_name` = COALESCE(`orders`.`receiver_name`, `snapshot`.`receiver_name`),
  `orders`.`receiver_mobile` = COALESCE(`orders`.`receiver_mobile`, `snapshot`.`receiver_mobile`),
  `orders`.`receiver_address` = COALESCE(`orders`.`receiver_address`, `snapshot`.`receiver_address`)
WHERE `orders`.`deleted` = b'0';

DROP TEMPORARY TABLE `tmp_xianyu_order_receiver_snapshot`;
