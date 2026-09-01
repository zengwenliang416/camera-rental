SET NAMES utf8mb4;

CREATE TABLE `xianyu_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `application_id` bigint NOT NULL,
  `external_shop_id` varchar(128) NOT NULL,
  `shop_name` varchar(256) DEFAULT NULL,
  `authorization_status` varchar(32) NOT NULL DEFAULT 'UNKNOWN',
  `authorization_expires_at` datetime DEFAULT NULL,
  `source_updated_at` datetime DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `xianyu_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `shop_id` bigint NOT NULL,
  `external_product_id` varchar(128) NOT NULL,
  `title` varchar(512) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'UNKNOWN',
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xianyu_product_shop_external`
    (`tenant_id`, `shop_id`, `external_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `xianyu_product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `product_id` bigint NOT NULL,
  `external_sku_id` varchar(128) NOT NULL DEFAULT '',
  `sku_name` varchar(512) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'UNKNOWN',
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xianyu_sku_product_external`
    (`tenant_id`, `product_id`, `external_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `xianyu_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `shop_id` bigint NOT NULL,
  `external_order_id` varchar(128) NOT NULL,
  `external_product_id` varchar(128) DEFAULT NULL,
  `external_sku_id` varchar(128) DEFAULT NULL,
  `goods_json` longtext,
  `source_updated_at` datetime DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xianyu_order_shop_external`
    (`tenant_id`, `shop_id`, `external_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `order_no` varchar(64) NOT NULL,
  `occupy_end_date_exclusive` date DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `system_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `permission` varchar(100) NOT NULL DEFAULT '',
  `type` tinyint NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `path` varchar(200) DEFAULT '',
  `icon` varchar(100) DEFAULT '#',
  `component` varchar(255) DEFAULT NULL,
  `component_name` varchar(255) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `visible` bit(1) NOT NULL DEFAULT b'1',
  `keep_alive` bit(1) NOT NULL DEFAULT b'1',
  `always_show` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `system_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `xianyu_shop` (
  `id`, `tenant_id`, `application_id`, `external_shop_id`,
  `shop_name`, `authorization_status`
) VALUES
  (10, 1, 1, 'shop-1', 'fixture shop', 'VALID');

INSERT INTO `xianyu_product` (
  `id`, `tenant_id`, `shop_id`, `external_product_id`, `title`
) VALUES
  (100, 1, 10, 'P-UNIQUE', 'unique product'),
  (101, 1, 10, 'P-AMBIG', 'ambiguous product'),
  (102, 1, 10, 'P-REVERSE-A', 'reverse ambiguous product A'),
  (103, 1, 10, 'P-REVERSE-B', 'reverse ambiguous product B'),
  (104, 1, 10, 'P-LEGACY-ONLY', 'legacy-only product'),
  (105, 1, 10, '1062409679830', 'numeric identifier product');

INSERT INTO `xianyu_product_sku` (
  `id`, `tenant_id`, `product_id`, `external_sku_id`, `sku_name`
) VALUES
  (200, 1, 100, 'SKU-UNIQUE', 'unique sku'),
  (201, 1, 101, 'SKU-AMBIG', 'ambiguous sku'),
  (202, 1, 104, 'SKU-LEGACY-ONLY', 'legacy-only sku');

INSERT INTO `xianyu_order` (
  `id`, `tenant_id`, `shop_id`, `external_order_id`,
  `external_product_id`, `external_sku_id`, `goods_json`, `source_updated_at`
) VALUES
  (1000, 1, 10, 'ORDER-UNIQUE', 'LEGACY-P-WRONG', 'LEGACY-S-WRONG',
   '{"product_id":"P-UNIQUE","item_id":"ITEM-UNIQUE","sku_id":"SKU-UNIQUE"}',
   '2026-08-31 10:00:00'),
  (1001, 1, 10, 'ORDER-AMBIG-A', 'P-AMBIG', 'SKU-AMBIG',
   '{"product_id":"P-AMBIG","item_id":"ITEM-AMBIG-A","sku_id":"SKU-AMBIG"}',
   '2026-08-31 10:01:00'),
  (1002, 1, 10, 'ORDER-AMBIG-B', 'P-AMBIG', 'SKU-AMBIG',
   '{"product_id":"P-AMBIG","item_id":"ITEM-AMBIG-B","sku_id":"SKU-AMBIG"}',
   '2026-08-31 10:02:00'),
  (1003, 1, 10, 'ORDER-REVERSE-A', 'P-REVERSE-A', NULL,
   '{"product_id":"P-REVERSE-A","item_id":"ITEM-SHARED"}',
   '2026-08-31 10:03:00'),
  (1004, 1, 10, 'ORDER-REVERSE-B', 'P-REVERSE-B', NULL,
   '{"product_id":"P-REVERSE-B","item_id":"ITEM-SHARED"}',
   '2026-08-31 10:04:00'),
  (1005, 1, 10, 'ORDER-INVALID-JSON', 'LEGACY-ONLY-P', 'LEGACY-ONLY-S',
   '{invalid-json',
   '2026-08-31 10:05:00'),
  (1006, 1, 10, 'ORDER-NUMERIC', NULL, NULL,
   '{"product_id":1062409679830,"item_id":9007199254740993,"sku_id":1061015327345}',
   '2026-08-31 10:06:00');

INSERT INTO `rental_order` (
  `id`, `tenant_id`, `order_no`, `occupy_end_date_exclusive`
) VALUES
  (500, 1, 'RENTAL-FIXTURE-1', '2026-09-10');
