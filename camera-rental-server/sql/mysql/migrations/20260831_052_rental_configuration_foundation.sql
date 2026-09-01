-- Explicit XianGuanJia/Xianyu identifiers and rental configuration foundation.
-- Legacy external_* columns remain historical evidence and are not used as order backfill sources.
SET NAMES utf8mb4;

ALTER TABLE `xianyu_shop`
  ADD COLUMN `xianyu_user_name` varchar(128) DEFAULT NULL
    COMMENT '闲鱼 user_name，用于商品 publish_shop 精确归属' AFTER `external_shop_id`,
  ADD KEY `idx_xianyu_shop_user_name` (`tenant_id`, `xianyu_user_name`, `id`);

ALTER TABLE `xianyu_product`
  MODIFY COLUMN `external_product_id` varchar(128) DEFAULT NULL
    COMMENT '历史含混商品标识，仅保留已有证据',
  ADD COLUMN `xgj_product_id` varchar(128) DEFAULT NULL
    COMMENT '闲管家商品 product_id' AFTER `shop_id`,
  ADD COLUMN `xianyu_item_id` varchar(128) DEFAULT NULL
    COMMENT '闲鱼商品 item_id' AFTER `xgj_product_id`;

ALTER TABLE `xianyu_product_sku`
  MODIFY COLUMN `external_sku_id` varchar(128) DEFAULT NULL
    COMMENT '历史含混规格标识，仅保留已有证据',
  ADD COLUMN `xgj_sku_id` varchar(128) DEFAULT NULL
    COMMENT '闲管家规格 sku_id' AFTER `product_id`,
  ADD COLUMN `xianyu_sku_id` varchar(128) DEFAULT NULL
    COMMENT '闲鱼规格 xy_sku_id' AFTER `xgj_sku_id`;

ALTER TABLE `xianyu_order`
  ADD COLUMN `xgj_product_id` varchar(128) DEFAULT NULL
    COMMENT '订单 goods.product_id' AFTER `external_order_id`,
  ADD COLUMN `xianyu_item_id` varchar(128) DEFAULT NULL
    COMMENT '订单 goods.item_id' AFTER `xgj_product_id`,
  ADD COLUMN `xgj_sku_id` varchar(128) DEFAULT NULL
    COMMENT '订单 goods.sku_id' AFTER `xianyu_item_id`,
  ADD COLUMN `xianyu_sku_id` varchar(128) DEFAULT NULL
    COMMENT '由同步规格唯一关系补充的 xy_sku_id' AFTER `xgj_sku_id`,
  ADD COLUMN `preparation_status` varchar(32) NOT NULL DEFAULT 'WAITING_RECONCILIATION'
    COMMENT '内部订单准备状态' AFTER `external_sku_id`,
  ADD COLUMN `preparation_reason_code` varchar(64) DEFAULT NULL
    COMMENT '准备状态原因码' AFTER `preparation_status`,
  ADD COLUMN `preparation_updated_at` datetime DEFAULT NULL
    COMMENT '准备状态更新时间' AFTER `preparation_reason_code`;

ALTER TABLE `rental_order`
  ADD COLUMN `preparation_status` varchar(32) NOT NULL DEFAULT 'WAITING_RECONCILIATION'
    COMMENT '设备分配与排期准备状态' AFTER `occupy_end_date_exclusive`,
  ADD COLUMN `preparation_reason_code` varchar(64) DEFAULT NULL
    COMMENT '准备状态原因码' AFTER `preparation_status`,
  ADD COLUMN `preparation_updated_at` datetime DEFAULT NULL
    COMMENT '准备状态更新时间' AFTER `preparation_reason_code`;

-- Product and SKU legacy columns have one proven source in their original persistence paths.
UPDATE `xianyu_product`
SET `xgj_product_id` = NULLIF(TRIM(`external_product_id`), '')
WHERE `xgj_product_id` IS NULL
  AND `external_product_id` IS NOT NULL;

UPDATE `xianyu_product_sku`
SET `xgj_sku_id` = NULLIF(TRIM(`external_sku_id`), '')
WHERE `xgj_sku_id` IS NULL
  AND `external_sku_id` IS NOT NULL;

-- Order legacy columns were historically populated by fallback, so only explicit goods JSON is safe.
UPDATE `xianyu_order`
SET `xgj_product_id` = CASE
      WHEN JSON_TYPE(JSON_EXTRACT(`goods_json`, '$.product_id')) IN ('INTEGER', 'STRING')
        THEN NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(`goods_json`, '$.product_id'))), '')
      ELSE NULL
    END,
    `xianyu_item_id` = CASE
      WHEN JSON_TYPE(JSON_EXTRACT(`goods_json`, '$.item_id')) IN ('INTEGER', 'STRING')
        THEN NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(`goods_json`, '$.item_id'))), '')
      ELSE NULL
    END,
    `xgj_sku_id` = CASE
      WHEN JSON_TYPE(JSON_EXTRACT(`goods_json`, '$.sku_id')) IN ('INTEGER', 'STRING')
        THEN NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(`goods_json`, '$.sku_id'))), '')
      ELSE NULL
    END
WHERE `goods_json` IS NOT NULL
  AND JSON_VALID(`goods_json`);

-- Backfill a product item only when both directions are unique inside the same tenant and shop.
UPDATE `xianyu_product` product
JOIN (
  SELECT by_product.`tenant_id`, by_product.`shop_id`, by_product.`xgj_product_id`,
         MIN(by_product.`xianyu_item_id`) AS `xianyu_item_id`
  FROM `xianyu_order` by_product
  JOIN (
    SELECT `tenant_id`, `shop_id`, `xianyu_item_id`
    FROM `xianyu_order`
    WHERE `deleted` = b'0'
      AND `xgj_product_id` IS NOT NULL
      AND `xianyu_item_id` IS NOT NULL
    GROUP BY `tenant_id`, `shop_id`, `xianyu_item_id`
    HAVING COUNT(DISTINCT `xgj_product_id`) = 1
  ) unique_item
    ON unique_item.`tenant_id` = by_product.`tenant_id`
   AND unique_item.`shop_id` = by_product.`shop_id`
   AND unique_item.`xianyu_item_id` = by_product.`xianyu_item_id`
  WHERE by_product.`deleted` = b'0'
    AND by_product.`xgj_product_id` IS NOT NULL
    AND by_product.`xianyu_item_id` IS NOT NULL
  GROUP BY by_product.`tenant_id`, by_product.`shop_id`, by_product.`xgj_product_id`
  HAVING COUNT(DISTINCT by_product.`xianyu_item_id`) = 1
) exact_product_item
  ON exact_product_item.`tenant_id` = product.`tenant_id`
 AND exact_product_item.`shop_id` = product.`shop_id`
 AND exact_product_item.`xgj_product_id` = product.`xgj_product_id`
SET product.`xianyu_item_id` = exact_product_item.`xianyu_item_id`
WHERE product.`xianyu_item_id` IS NULL
  AND product.`deleted` = b'0';

-- The order payload does not contain xy_sku_id; enrich only through an exact synchronized SKU row.
UPDATE `xianyu_order` channel_order
JOIN `xianyu_product` product
  ON product.`tenant_id` = channel_order.`tenant_id`
 AND product.`shop_id` = channel_order.`shop_id`
 AND product.`xgj_product_id` = channel_order.`xgj_product_id`
 AND product.`deleted` = b'0'
JOIN `xianyu_product_sku` product_sku
  ON product_sku.`tenant_id` = channel_order.`tenant_id`
 AND product_sku.`product_id` = product.`id`
 AND product_sku.`xgj_sku_id` = channel_order.`xgj_sku_id`
 AND product_sku.`deleted` = b'0'
SET channel_order.`xianyu_sku_id` = product_sku.`xianyu_sku_id`
WHERE channel_order.`xianyu_sku_id` IS NULL
  AND product_sku.`xianyu_sku_id` IS NOT NULL
  AND channel_order.`deleted` = b'0';

ALTER TABLE `xianyu_product`
  ADD UNIQUE KEY `uk_xianyu_product_shop_xgj` (`tenant_id`, `shop_id`, `xgj_product_id`),
  ADD UNIQUE KEY `uk_xianyu_product_shop_item` (`tenant_id`, `shop_id`, `xianyu_item_id`);

ALTER TABLE `xianyu_product_sku`
  ADD UNIQUE KEY `uk_xianyu_sku_product_xgj` (`tenant_id`, `product_id`, `xgj_sku_id`),
  ADD UNIQUE KEY `uk_xianyu_sku_product_xianyu` (`tenant_id`, `product_id`, `xianyu_sku_id`);

ALTER TABLE `xianyu_order`
  ADD KEY `idx_xianyu_order_exact_model` (
    `tenant_id`, `shop_id`, `xianyu_item_id`, `xgj_sku_id`, `id`
  ),
  ADD KEY `idx_xianyu_order_preparation` (
    `tenant_id`, `preparation_status`, `source_updated_at`, `id`
  );

ALTER TABLE `rental_order`
  ADD KEY `idx_rental_order_preparation` (
    `tenant_id`, `preparation_status`, `update_time`, `id`
  );

CREATE TABLE IF NOT EXISTS `rental_channel_product_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `shop_id` bigint NOT NULL,
  `xianyu_item_id` varchar(128) NOT NULL,
  `xgj_product_id` varchar(128) DEFAULT NULL,
  `product_title_snapshot` varchar(512) DEFAULT NULL,
  `handling_policy` varchar(32) NOT NULL,
  `mapping_mode` varchar(32) NOT NULL DEFAULT 'SINGLE',
  `single_device_model_id` bigint DEFAULT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `rule_note` varchar(512) DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_channel_rule_shop_item` (`tenant_id`, `shop_id`, `xianyu_item_id`),
  KEY `idx_rental_channel_rule_policy` (`tenant_id`, `handling_policy`, `enabled`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁渠道商品处理与型号规则';

CREATE TABLE IF NOT EXISTS `rental_channel_product_sku_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `product_rule_id` bigint NOT NULL,
  `product_sku_id` bigint NOT NULL,
  `xgj_sku_id` varchar(128) NOT NULL,
  `xianyu_sku_id` varchar(128) DEFAULT NULL,
  `device_model_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `lock_version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_channel_sku_rule_xgj` (`tenant_id`, `product_rule_id`, `xgj_sku_id`),
  UNIQUE KEY `uk_rental_channel_sku_rule_source` (`tenant_id`, `product_rule_id`, `product_sku_id`),
  KEY `idx_rental_channel_sku_model` (`tenant_id`, `device_model_id`, `enabled`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁渠道商品 SKU 精确型号映射';

CREATE TABLE IF NOT EXISTS `xianyu_order_remark_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `xianyu_order_id` bigint NOT NULL,
  `raw_payload_id` bigint DEFAULT NULL,
  `seller_remark` text,
  `parse_version` varchar(64) DEFAULT NULL,
  `parse_status` varchar(32) NOT NULL,
  `parse_reason_code` varchar(64) DEFAULT NULL,
  `ship_date` date DEFAULT NULL,
  `receive_date` date DEFAULT NULL,
  `billable_start_date` date DEFAULT NULL,
  `billable_end_date` date DEFAULT NULL,
  `send_back_date` date DEFAULT NULL,
  `effective_plan` bit(1) NOT NULL DEFAULT b'0',
  `change_type` varchar(32) DEFAULT NULL,
  `source_updated_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_xianyu_remark_history_order` (`tenant_id`, `xianyu_order_id`, `create_time`, `id`),
  KEY `idx_xianyu_remark_history_effective` (`tenant_id`, `xianyu_order_id`, `effective_plan`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲鱼订单备注解析与有效计划历史';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7110, 'router.rentalConfiguration', '', 2, 10, 7000,
  'configuration', 'ep:setting', 'rental/configuration/index', 'RentalConfiguration', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7110);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7111, '租赁配置查询', 'rental:configuration:query', 3, 1, 7110,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7111);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7112, '租赁配置更新', 'rental:configuration:update', 3, 2, 7110,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7112);
